use std::str::FromStr;

use android_activity::AndroidApp;
use ruffle_core::backend::ui::{LanguageIdentifier, UiBackend};
use url::Url;

#[derive(Clone)]
pub struct AndroidUiBackend {
    app: AndroidApp,
}

impl AndroidUiBackend {
    pub fn new(app: AndroidApp) -> Self {
        Self { app }
    }
}

impl UiBackend for AndroidUiBackend {
    fn mouse_visible(&self) -> bool {
        false
    }

    fn set_mouse_visible(&mut self, visible: bool) {}

    fn set_mouse_cursor(&mut self, cursor: ruffle_core::backend::ui::MouseCursor) {}

    fn clipboard_content(&mut self) -> String {
        "".into()
    }

    fn set_clipboard_content(&mut self, content: String) {}

    fn set_fullscreen(
        &mut self,
        is_full: bool,
    ) -> Result<(), ruffle_core::backend::ui::FullscreenError> {
        Ok(())
    }

    fn display_root_movie_download_failed_message(
        &self,
        _invalid_swf: bool,
        _fetched_error: String,
    ) {
    }

    fn message(&self, message: &str) {}

    fn open_virtual_keyboard(&self) {
        self.app.show_soft_input(false);
    }

    fn close_virtual_keyboard(&self) {
        self.app.hide_soft_input(false);
    }

    fn language(&self) -> ruffle_core::backend::ui::LanguageIdentifier {
        LanguageIdentifier::from_str("en-US").unwrap()
    }

    fn display_unsupported_video(&self, url: Url) {}

    fn load_device_font(
        &self,
        query: &ruffle_core::FontQuery,
        register: &mut dyn FnMut(ruffle_core::backend::ui::FontDefinition),
    ) {
    }

    fn sort_device_fonts(
        &self,
        query: &ruffle_core::FontQuery,
        register: &mut dyn FnMut(ruffle_core::backend::ui::FontDefinition),
    ) -> Vec<ruffle_core::FontQuery> {
        Vec::new()
    }

    fn display_file_open_dialog(
        &mut self,
        filters: Vec<ruffle_core::backend::ui::FileFilter>,
    ) -> Option<ruffle_core::backend::ui::DialogResultFuture> {
        None
    }

    fn display_file_save_dialog(
        &mut self,
        file_name: String,
        title: String,
    ) -> Option<ruffle_core::backend::ui::DialogResultFuture> {
        None
    }

    fn close_file_dialog(&mut self) {}
}
