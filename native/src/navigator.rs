//! Navigator backend for Android

use crate::custom_event::RuffleEvent;

use ruffle_core::backend::navigator::{
    async_return, create_fetch_error, resolve_url_with_relative_base_path, ErrorResponse,
    NavigationMethod, NavigatorBackend, OpenURLMode, OwnedFuture, Request, SuccessResponse,
};
use ruffle_core::indexmap::IndexMap;
use ruffle_core::loader::Error;
use ruffle_core::socket::{ConnectionState, SocketAction, SocketHandle};

use std::sync::mpsc::Sender;
use async_channel::Receiver;
use std::time::Duration;

use url::{ParseError, Url};

use winit::event_loop::EventLoopProxy;

use jni::objects::JValue;

use crate::get_jvm;

/// Implementation of `NavigatorBackend` for Android.
pub struct ExternalNavigatorBackend {
    /// Sink for tasks sent to us through `spawn_future`.
    channel: Sender<OwnedFuture<(), Error>>,

    /// Event sink to trigger a new task poll.
    event_loop: EventLoopProxy<RuffleEvent>,

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
        event_loop: EventLoopProxy<RuffleEvent>,
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
        let bytes = env
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

    fn fetch(&self, request: Request) -> OwnedFuture<SuccessResponse, ErrorResponse> {
        // TODO: honor sandbox type (local-with-filesystem, local-with-network, remote, ...)
        let mut processed_url = match self.resolve_url(request.url()) {
            Ok(url) => url,
            Err(e) => {
                return async_return(create_fetch_error(request.url(), e));
            }
        };

        Box::pin(async move {
            Err(ErrorResponse {
                url: processed_url.to_string(),
                error: Error::FetchError("Network unavailable".to_string()),
            })
        })

        /*
        match processed_url.scheme() {
            "file" => Box::pin(async move {
                let path = processed_url.to_file_path().unwrap_or_default();

                //let url = processed_url.into();

                Err(Error::FetchError(
                    "No 'file:' protocol support yet".to_string(),
                ))
            }),
            _ => Box::pin(
                async move { Err(Error::FetchError("No network support yet".to_string())) },
            ),
        }
        */
    }

    fn spawn_future(&mut self, future: OwnedFuture<(), Error>) {
        self.channel.send(future).expect("working channel send");

        if self.event_loop.send_event(RuffleEvent::TaskPoll).is_err() {
            log::warn!(
                "A task was queued on an event loop that has already ended. It will not be polled."
            );
        }
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
            .send(SocketAction::Connect(handle, ConnectionState::Failed))
            .expect("working channel send");
    }
}
