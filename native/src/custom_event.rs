//! Custom event type for Ruffle on Android

/// User-defined events.
pub enum RuffleEvent {
    /// Indicates that one or more tasks are ready to poll on our executor.
    TaskPoll,
}
