use std::cell::{Cell, RefCell};
use std::rc::Rc;
use std::str::FromStr;

use android_activity::input::TextInputState;
use android_activity::AndroidApp;
use ruffle_core::backend::ui::{LanguageIdentifier, UiBackend};
use url::Url;

use crate::ime::ImeState;

#[derive(Clone)]
pub struct AndroidUiBackend {
    app: AndroidApp,
    ime: Rc<RefCell<ImeState>>,
    keyboard_open: Rc<Cell<bool>>,
}

impl AndroidUiBackend {
    pub fn new(app: AndroidApp, ime: Rc<RefCell<ImeState>>) -> Self {
        Self {
            app,
            ime,
            keyboard_open: Rc::new(Cell::new(false)),
        }
    }
}

impl UiBackend for AndroidUiBackend {
    fn mouse_visible(&self) -> bool {
        false
    }

    fn set_mouse_visible(&mut self, _visible: bool) {}

    fn set_mouse_cursor(&mut self, _cursor: ruffle_core::backend::ui::MouseCursor) {}

    fn clipboard_content(&mut self) -> String {
        "".into()
    }

    fn set_clipboard_content(&mut self, _content: String) {}

    fn set_fullscreen(
        &mut self,
        _is_full: bool,
    ) -> Result<(), ruffle_core::backend::ui::FullscreenError> {
        Ok(())
    }

    fn display_root_movie_download_failed_message(
        &self,
        _invalid_swf: bool,
        _fetched_error: String,
    ) {
    }

    fn message(&self, _message: &str) {}

    fn open_virtual_keyboard(&self) {
        // This is called on every focus update, even if the same text field stays
        // focused, and resetting the IME would cancel any composition in progress.
        if !self.keyboard_open.replace(true) {
            *self.ime.borrow_mut() = ImeState::default();
            self.app.set_text_input_state(TextInputState::default());
        }
        self.app.show_soft_input(false);
    }

    fn close_virtual_keyboard(&self) {
        self.keyboard_open.set(false);
        self.app.hide_soft_input(false);
    }

    fn language(&self) -> ruffle_core::backend::ui::LanguageIdentifier {
        LanguageIdentifier::from_str("en-US").unwrap()
    }

    fn display_unsupported_video(&self, _url: Url) {}

    fn load_device_font(
        &self,
        _query: &ruffle_core::font::FontQuery,
        _register: &mut dyn FnMut(ruffle_core::backend::ui::FontDefinition),
    ) {
    }

    fn sort_device_fonts(
        &self,
        _query: &ruffle_core::font::FontQuery,
        _register: &mut dyn FnMut(ruffle_core::backend::ui::FontDefinition),
    ) -> Vec<ruffle_core::font::FontQuery> {
        Vec::new()
    }

    fn display_file_open_dialog(
        &mut self,
        _filters: Vec<ruffle_core::backend::ui::FileFilter>,
    ) -> Option<ruffle_core::backend::ui::DialogResultFuture> {
        None
    }

    fn display_file_open_dialog_multiple(
        &mut self,
        _filters: Vec<ruffle_core::backend::ui::FileFilter>,
    ) -> Option<ruffle_core::backend::ui::MultiDialogResultFuture> {
        None
    }

    fn display_file_save_dialog(
        &mut self,
        _file_name: String,
        _title: String,
    ) -> Option<ruffle_core::backend::ui::DialogResultFuture> {
        None
    }

    fn close_file_dialog(&mut self) {}
}
