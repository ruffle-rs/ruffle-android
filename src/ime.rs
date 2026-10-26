//! Translates GameActivity's text input state into Ruffle IME events.
//!
//! GameActivity doesn't report incremental edits made by the soft keyboard;
//! instead, every `TextEvent` carries the whole contents of its editing buffer,
//! along with the selection and the composing region. We keep the last state
//! around, and turn the difference into backspaces, commits and preedit updates,
//! assuming that the caret of the focused text field is right after the text
//! that was typed into it via the IME.
//!
//! The buffer can't simply be cleared after each commit, because setting it
//! restarts the IME, cancelling any composition in progress.

use android_activity::input::TextInputState;
use ruffle_core::events::{ImeEvent, PlayerEvent, TextControlCode};

#[derive(Default)]
pub struct ImeState {
    /// The part of the IME buffer outside the composing region.
    /// All of it has already been committed to the player.
    committed: String,

    /// The contents of the composing region, currently shown as preedit.
    preedit: String,

    /// The selection within `preedit`, in bytes.
    preedit_cursor: Option<(usize, usize)>,
}

impl ImeState {
    /// Updates the state to match `state`, and returns the events that bring
    /// the focused text field in line with it.
    pub fn update(&mut self, state: &TextInputState) -> Vec<PlayerEvent> {
        let text = state.text.as_str();
        let (committed, preedit, preedit_cursor) = match state.compose_region {
            Some(region) => {
                let start = byte_index(text, region.start.min(region.end));
                let end = byte_index(text, region.start.max(region.end));
                let sel_start = byte_index(text, state.selection.start.min(state.selection.end));
                let sel_end = byte_index(text, state.selection.start.max(state.selection.end));
                let cursor = (start <= sel_start && sel_end <= end)
                    .then(|| (sel_start - start, sel_end - start));
                (
                    [&text[..start], &text[end..]].concat(),
                    text[start..end].to_owned(),
                    cursor,
                )
            }
            None => (text.to_owned(), String::new(), None),
        };

        let common = common_prefix_len(&self.committed, &committed);
        let deleted = self.committed[common..].chars().count();
        let inserted = &committed[common..];
        let committed_changed = deleted > 0 || !inserted.is_empty();

        let mut events = Vec::new();
        // Backspaces and commits act at the caret, so the preedit has to go first.
        if !self.preedit.is_empty() && (committed_changed || preedit.is_empty()) {
            events.push(PlayerEvent::Ime(ImeEvent::Preedit(String::new(), None)));
        }
        events.extend((0..deleted).map(|_| PlayerEvent::TextControl {
            code: TextControlCode::Backspace,
        }));
        if !inserted.is_empty() {
            events.push(PlayerEvent::Ime(ImeEvent::Commit(inserted.to_owned())));
        }
        if !preedit.is_empty()
            && (committed_changed
                || preedit != self.preedit
                || preedit_cursor != self.preedit_cursor)
        {
            events.push(PlayerEvent::Ime(ImeEvent::Preedit(
                preedit.clone(),
                preedit_cursor,
            )));
        }

        *self = Self {
            committed,
            preedit,
            preedit_cursor,
        };
        events
    }
}

/// Converts an index in UTF-16 code units (as used by Java) to a byte index into `text`.
fn byte_index(text: &str, utf16_index: usize) -> usize {
    let mut units = 0;
    for (i, c) in text.char_indices() {
        if units >= utf16_index {
            return i;
        }
        units += c.len_utf16();
    }
    text.len()
}

/// Returns the length in bytes of the longest common prefix of `a` and `b`.
fn common_prefix_len(a: &str, b: &str) -> usize {
    a.char_indices()
        .zip(b.chars())
        .find(|((_, x), y)| x != y)
        .map_or(a.len().min(b.len()), |((i, _), _)| i)
}

#[cfg(test)]
mod tests {
    use super::*;
    use android_activity::input::TextSpan;

    fn state(text: &str, cursor: usize, compose: Option<(usize, usize)>) -> TextInputState {
        TextInputState {
            text: text.to_owned(),
            selection: TextSpan {
                start: cursor,
                end: cursor,
            },
            compose_region: compose.map(|(start, end)| TextSpan { start, end }),
        }
    }

    fn preedit(text: &str, cursor: usize) -> PlayerEvent {
        PlayerEvent::Ime(ImeEvent::Preedit(text.to_owned(), Some((cursor, cursor))))
    }

    fn clear_preedit() -> PlayerEvent {
        PlayerEvent::Ime(ImeEvent::Preedit(String::new(), None))
    }

    fn commit(text: &str) -> PlayerEvent {
        PlayerEvent::Ime(ImeEvent::Commit(text.to_owned()))
    }

    fn backspace() -> PlayerEvent {
        PlayerEvent::TextControl {
            code: TextControlCode::Backspace,
        }
    }

    fn assert_events(actual: Vec<PlayerEvent>, expected: Vec<PlayerEvent>) {
        assert_eq!(format!("{actual:?}"), format!("{expected:?}"));
    }

    #[test]
    fn compose_then_commit_word() {
        let mut ime = ImeState::default();
        assert_events(
            ime.update(&state("h", 1, Some((0, 1)))),
            vec![preedit("h", 1)],
        );
        assert_events(
            ime.update(&state("hi", 2, Some((0, 2)))),
            vec![preedit("hi", 2)],
        );
        assert_events(
            ime.update(&state("hi ", 3, None)),
            vec![clear_preedit(), commit("hi ")],
        );
        assert_events(
            ime.update(&state("hi y", 4, Some((3, 4)))),
            vec![preedit("y", 1)],
        );
    }

    #[test]
    fn plain_commits_are_not_repeated() {
        let mut ime = ImeState::default();
        assert_events(ime.update(&state("a", 1, None)), vec![commit("a")]);
        assert_events(ime.update(&state("ab", 2, None)), vec![commit("b")]);
        assert_events(ime.update(&state("abc", 3, None)), vec![commit("c")]);
    }

    #[test]
    fn deleting_committed_text() {
        let mut ime = ImeState::default();
        ime.update(&state("abc", 3, None));
        assert_events(ime.update(&state("ab", 2, None)), vec![backspace()]);
    }

    #[test]
    fn recomposing_committed_word() {
        let mut ime = ImeState::default();
        ime.update(&state("hello ", 6, None));
        // Backspacing into a word makes Gboard turn it back into a composition.
        assert_events(
            ime.update(&state("hello", 5, Some((0, 5)))),
            vec![
                backspace(),
                backspace(),
                backspace(),
                backspace(),
                backspace(),
                backspace(),
                preedit("hello", 5),
            ],
        );
    }

    #[test]
    fn autocorrect_replaces_composition() {
        let mut ime = ImeState::default();
        ime.update(&state("teh", 3, Some((0, 3))));
        assert_events(
            ime.update(&state("the ", 4, None)),
            vec![clear_preedit(), commit("the ")],
        );
    }

    #[test]
    fn cancelling_composition() {
        let mut ime = ImeState::default();
        ime.update(&state("x", 1, Some((0, 1))));
        assert_events(ime.update(&state("", 0, None)), vec![clear_preedit()]);
    }

    #[test]
    fn indices_are_utf16() {
        let mut ime = ImeState::default();
        ime.update(&state("🙂", 2, None));
        // "é" is 1 UTF-16 unit but 2 bytes; "🙂" is 2 units but 4 bytes.
        assert_events(
            ime.update(&state("🙂é", 3, Some((2, 3)))),
            vec![preedit("é", 2)],
        );
        ime.update(&state("🙂é", 3, None));
        assert_events(ime.update(&state("🙂", 2, None)), vec![backspace()]);
        assert_events(ime.update(&state("", 0, None)), vec![backspace()]);
    }
}
