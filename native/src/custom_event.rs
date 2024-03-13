//! Custom event type for Ruffle on Android

use ruffle_core::events::KeyCode;
use ruffle_core::ViewportDimensions;

/// User-defined events.
#[derive(Debug)]
pub enum RuffleEvent {
    /// Indicates that one or more tasks are ready to poll on our executor.
    TaskPoll,
    VirtualKeyEvent {
        down: bool,
        key_code: KeyCode,
        key_char: Option<char>,
    },
    RunContextMenuCallback(usize),
    ClearContextMenu,
    Resize(ViewportDimensions),
    RequestContextMenu,
}
