//! Custom event type for Ruffle on Android

use ruffle_core::events::KeyDescriptor;

use crate::PlayerRunnable;

/// User-defined events.
pub enum RuffleEvent {
    /// Indicates that a task is ready to be polled.
    TaskPoll(PlayerRunnable),
    VirtualKeyEvent {
        down: bool,
        key_descriptor: KeyDescriptor,
    },
    RunContextMenuCallback(usize),
    ClearContextMenu,
    RequestContextMenu,
    /// Show the menu of the scene button.
    RequestSceneMenu,
    /// Jump the root timeline to this 1-based frame, picked from the scene menu.
    GotoRootFrame(u16),
    /// Switch to the scene at this index in the game's own scene list.
    GotoGameScene(usize),
}
