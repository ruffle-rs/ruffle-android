use ruffle_frontend_utils::backends::navigator::NavigatorInterface;
use std::fs::File;
use std::io;
use std::path::Path;
use url::Url;

#[derive(Clone)]
pub struct AndroidNavigatorInterface;

// TODO: Prompt the user for these things!
impl NavigatorInterface for AndroidNavigatorInterface {
    fn navigate_to_website(&self, url: Url, _ask: bool) {
        // TODO: Implement `ask` behavior!
        let _ = webbrowser::open(url.as_ref());
    }

    async fn open_file(&self, path: &Path) -> io::Result<File> {
        File::open(path)
    }

    async fn confirm_socket(&self, _host: &str, _port: u16) -> bool {
        true
    }
}
