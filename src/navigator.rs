use android_activity::AndroidApp;
use ruffle_frontend_utils::backends::navigator::NavigatorInterface;
use std::fs::File;
use std::io;
use std::os::fd::FromRawFd;
use std::path::Path;
use url::Url;

use crate::java::JavaInterface;
use crate::with_activity;

/// Where a folder opened by the user is mounted, so that a movie in it gets a
/// `file://` URL, and the files it loads with relative URLs resolve within it.
const FOLDER_ROOT: &str = "/ruffle-folder";

/// The `file://` URL of the file at `path` within the folder opened by the user.
pub fn folder_file_url(path: &str) -> Url {
    Url::from_file_path(Path::new(FOLDER_ROOT).join(path))
        .expect("Path within the folder root should be absolute")
}

#[derive(Clone)]
pub struct AndroidNavigatorInterface {
    app: AndroidApp,
}

impl AndroidNavigatorInterface {
    pub fn new(app: AndroidApp) -> Self {
        Self { app }
    }

    fn open_folder_file(&self, path: &Path) -> io::Result<File> {
        let path = path
            .to_str()
            .ok_or_else(|| io::Error::from(io::ErrorKind::InvalidInput))?;
        let fd = with_activity(&self.app, |env, activity| {
            JavaInterface::open_folder_file(env, activity, path)
        })
        .map_err(|e| io::Error::other(e.to_string()))?;
        if fd < 0 {
            return Err(io::ErrorKind::NotFound.into());
        }
        // SAFETY: `openFolderFile` detaches the descriptor from Java, so it's ours to own and close.
        Ok(unsafe { File::from_raw_fd(fd) })
    }
}

// TODO: Prompt the user for these things!
impl NavigatorInterface for AndroidNavigatorInterface {
    fn navigate_to_website(&self, url: Url) {
        // TODO: Implement `ask` behavior!
        let _ = webbrowser::open(url.as_ref());
    }

    async fn open_file(&self, path: &Path) -> io::Result<File> {
        match path.strip_prefix(FOLDER_ROOT) {
            Ok(path_in_folder) => self.open_folder_file(path_in_folder),
            Err(_) => File::open(path),
        }
    }

    async fn confirm_socket(&self, _host: &str, _port: u16) -> bool {
        true
    }
}
