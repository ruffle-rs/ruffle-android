//! Navigator backend for Android

use crate::custom_event::RuffleEvent;
use std::borrow::Cow;
use std::fs::File;
use std::io::Read;
use std::sync::{Arc, Mutex};

use ruffle_core::backend::navigator::{
    async_return, create_fetch_error, create_specific_fetch_error, ErrorResponse, NavigationMethod,
    NavigatorBackend, OpenURLMode, OwnedFuture, Request, SuccessResponse,
};
use ruffle_core::indexmap::IndexMap;
use ruffle_core::loader::Error;
use ruffle_core::socket::{ConnectionState, SocketAction, SocketHandle};

use async_channel::{Receiver, Sender};
use std::time::Duration;

use url::{ParseError, Url};

use jni::objects::JValue;

use crate::{get_jvm, EventSender};

/// Implementation of `NavigatorBackend` for Android.
pub struct ExternalNavigatorBackend {
    /// Sink for tasks sent to us through `spawn_future`.
    channel: Sender<OwnedFuture<(), Error>>,

    /// Event sink to trigger a new task poll.
    event_loop: EventSender,

    /// The url to use for all relative fetches.
    base_url: Url,

    upgrade_to_https: bool,

    open_url_mode: OpenURLMode,
}

impl ExternalNavigatorBackend {
    /// Construct a navigator backend with fetch and async capability.
    pub fn new(
        mut base_url: Url,
        channel: Sender<OwnedFuture<(), Error>>,
        event_loop: EventSender,
        upgrade_to_https: bool,
        open_url_mode: OpenURLMode,
    ) -> Self {
        // Force replace the last segment with empty. //

        if let Ok(mut base_url) = base_url.path_segments_mut() {
            base_url.pop().pop_if_empty().push("");
        }

        Self {
            channel,
            event_loop,
            base_url,
            upgrade_to_https,
            open_url_mode,
        }
    }
}

impl NavigatorBackend for ExternalNavigatorBackend {
    fn navigate_to_url(
        &self,
        url: &str,
        _target: &str,
        vars_method: Option<(NavigationMethod, IndexMap<String, String>)>,
    ) {
        //TODO: Should we return a result for failed opens? Does Flash care?

        //NOTE: Flash desktop players / projectors ignore the window parameter,
        //      unless it's a `_layer`, and we shouldn't handle that anyway.
        let mut parsed_url = match self.base_url.join(url) {
            Ok(parsed_url) => parsed_url,
            Err(e) => {
                log::error!(
                    "Could not parse URL because of {}, the corrupt URL was: {}",
                    e,
                    url
                );
                return;
            }
        };

        let modified_url = match vars_method {
            Some((_, query_pairs)) => {
                {
                    //lifetime limiter because we don't have NLL yet
                    let mut modifier = parsed_url.query_pairs_mut();

                    for (k, v) in query_pairs.iter() {
                        modifier.append_pair(k, v);
                    }
                }

                parsed_url
            }
            None => parsed_url,
        };

        let processed_url = self.pre_process_url(modified_url);

        if processed_url.scheme() == "javascript" {
            log::warn!(
                "SWF tried to run a script on desktop, but javascript calls are not allowed"
            );
            return;
        }

        if self.open_url_mode == OpenURLMode::Confirm {
            // TODO
        } else if self.open_url_mode == OpenURLMode::Deny {
            log::warn!("SWF tried to open a website, but opening a website is not allowed");
            return;
        }

        log::warn!("navigating to url {}", processed_url);
        let (jvm, activity) = get_jvm().unwrap();
        let mut env = jvm.attach_current_thread().unwrap();

        // no worky :(
        //ndk_glue::native_activity().show_soft_input(true);

        let java_url_string = env.new_string(processed_url.to_string()).unwrap();
        let _ = env
            .call_method(
                &activity,
                "navigateToUrl",
                "(Ljava/lang/String;)V",
                &[JValue::Object(&java_url_string)],
            )
            .unwrap();
    }

    fn resolve_url(&self, url: &str) -> Result<Url, ParseError> {
        match self.base_url.join(url) {
            Ok(url) => Ok(self.pre_process_url(url)),
            Err(error) => Err(error),
        }
    }

    fn fetch(&self, request: Request) -> OwnedFuture<Box<dyn SuccessResponse>, ErrorResponse> {
        // TODO: honor sandbox type (local-with-filesystem, local-with-network, remote, ...)
        let mut processed_url = match self.resolve_url(request.url()) {
            Ok(url) => url,
            Err(e) => {
                return async_return(create_fetch_error(request.url(), e));
            }
        };

        // TODO: Handle content: schemes

        enum AndroidResponseBody {
            File(File),
            Network(Arc<Mutex<Box<dyn Read + Send + Sync + 'static>>>),
        }

        struct AndroidResponse {
            redirected: bool,
            status: u16,
            url: String,
            response_body: AndroidResponseBody,
            length: Option<u64>,
        }

        impl SuccessResponse for AndroidResponse {
            fn url(&self) -> Cow<str> {
                Cow::Borrowed(&self.url)
            }

            fn body(self: Box<Self>) -> OwnedFuture<Vec<u8>, Error> {
                let length = self.length.unwrap_or_default() as usize;
                match self.response_body {
                    AndroidResponseBody::File(mut file) => Box::pin(async move {
                        let mut body = vec![];
                        file.read_to_end(&mut body)
                            .map_err(|e| Error::FetchError(e.to_string()))?;

                        Ok(body)
                    }),
                    AndroidResponseBody::Network(response) => Box::pin(async move {
                        let mut bytes: Vec<u8> = Vec::with_capacity(length);
                        response
                            .lock()
                            .expect("working lock during fetch body read")
                            .read_to_end(&mut bytes)?;
                        Ok(bytes)
                    }),
                }
            }

            fn status(&self) -> u16 {
                self.status
            }

            fn redirected(&self) -> bool {
                self.redirected
            }

            fn next_chunk(&mut self) -> OwnedFuture<Option<Vec<u8>>, Error> {
                match &mut self.response_body {
                    AndroidResponseBody::File(file) => {
                        let mut buf = vec![0; 4096];
                        let res = file.read(&mut buf);

                        Box::pin(async move {
                            match res {
                                Ok(count) if count > 0 => {
                                    buf.resize(count, 0);
                                    Ok(Some(buf))
                                }
                                Ok(_) => Ok(None),
                                Err(e) => Err(Error::FetchError(e.to_string())),
                            }
                        })
                    }
                    AndroidResponseBody::Network(response) => {
                        let reader = response.clone();
                        Box::pin(async move {
                            let mut buf = vec![0; 4096];
                            let lock = reader.try_lock();
                            if matches!(lock, Err(std::sync::TryLockError::WouldBlock)) {
                                return Err(Error::FetchError(
                            "Concurrent read operations on the same stream are not supported."
                                .to_string(),
                        ));
                            }
                            let result = lock.expect("network lock").read(&mut buf);

                            match result {
                                Ok(count) if count > 0 => {
                                    buf.resize(count, 0);
                                    Ok(Some(buf))
                                }
                                Ok(_) => Ok(None),
                                Err(e) => Err(Error::FetchError(e.to_string())),
                            }
                        })
                    }
                }
            }

            fn expected_length(&self) -> Result<Option<u64>, Error> {
                Ok(self.length)
            }
        }

        match processed_url.scheme() {
            "file" => Box::pin(async move {
                // We send the original url (including query parameters)
                // back to ruffle_core in the `Response`
                let response_url = processed_url.clone();
                // Flash supports query parameters with local urls.
                // SwfMovie takes care of exposing those to ActionScript -
                // when we actually load a filesystem url, strip them out.
                processed_url.set_query(None);

                let path = match processed_url.to_file_path() {
                    Ok(path) => path,
                    Err(_) => {
                        return create_specific_fetch_error(
                            "Unable to create path out of URL",
                            response_url.as_str(),
                            "",
                        );
                    }
                };

                let contents = std::fs::File::open(path);

                let file = match contents {
                    Ok(file) => file,
                    Err(e) => {
                        return create_specific_fetch_error(
                            "Can't open file",
                            response_url.as_str(),
                            e,
                        );
                    }
                };

                let length = file.metadata().map(|m| m.len()).ok();
                let response: Box<dyn SuccessResponse> = Box::new(AndroidResponse {
                    url: response_url.to_string(),
                    response_body: AndroidResponseBody::File(file),
                    status: 0,
                    redirected: false,
                    length,
                });

                Ok(response)
            }),
            _ => Box::pin(async move {
                let mut ureq_request = ureq::request_url(
                    match request.method() {
                        NavigationMethod::Get => "GET",
                        NavigationMethod::Post => "POST",
                    },
                    &processed_url,
                );

                let (body_data, mime) = request.body().clone().unwrap_or_default();
                for (name, val) in request.headers().iter() {
                    ureq_request = ureq_request.set(name, val);
                }
                ureq_request = ureq_request.set("Content-Type", &mime);
                let response = ureq_request.send_bytes(&body_data).map_err(|e| {
                    log::warn!("Error fetching url: {e}");
                    let inner = match e.kind() {
                        ureq::ErrorKind::Dns => Error::InvalidDomain(processed_url.to_string()),
                        _ => Error::FetchError(e.to_string()),
                    };
                    ErrorResponse {
                        url: processed_url.to_string(),
                        error: inner,
                    }
                })?;

                let status = response.status();
                let redirected = response.get_url() != processed_url.as_str();
                let response_length = response
                    .header("Content-Length")
                    .and_then(|s| s.parse::<u64>().ok());

                if !(200..300).contains(&status) {
                    let error = Error::HttpNotOk(
                        format!("HTTP status is not ok, got {}", response.status()),
                        status,
                        redirected,
                        response_length.unwrap_or_default(),
                    );
                    return Err(ErrorResponse {
                        url: response.get_url().to_string(),
                        error,
                    });
                }

                let response: Box<dyn SuccessResponse> = Box::new(AndroidResponse {
                    url: response.get_url().to_string(),
                    response_body: AndroidResponseBody::Network(Arc::new(Mutex::new(
                        response.into_reader(),
                    ))),
                    status,
                    redirected,
                    length: response_length,
                });
                Ok(response)
            }),
        }
    }

    fn spawn_future(&mut self, future: OwnedFuture<(), Error>) {
        self.channel
            .send_blocking(future)
            .expect("Channel must accept new futures");
        self.event_loop.send(RuffleEvent::TaskPoll);
    }

    fn pre_process_url(&self, mut url: Url) -> Url {
        if self.upgrade_to_https && url.scheme() == "http" && url.set_scheme("https").is_err() {
            log::error!("Url::set_scheme failed on: {}", url);
        }
        url
    }

    fn connect_socket(
        &mut self,
        _host: String,
        _port: u16,
        _timeout: Duration,
        handle: SocketHandle,
        _receiver: Receiver<Vec<u8>>,
        sender: Sender<SocketAction>,
    ) {
        sender
            .try_send(SocketAction::Connect(handle, ConnectionState::Failed))
            .expect("Channel must accept results");
    }
}
