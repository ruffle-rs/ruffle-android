# Notes on this fork

This fork of Ruffle's Android app adds a way to jump between the scenes of a movie, and
loads the files a movie needs from the folder it's in. The Ruffle side, the games' engine,
the builds and the analysis script are described in the notes of the Ruffle fork:
https://github.com/VlasenkoMykola/Ruffle_fork/blob/claude/relaxed-gauss-ymscnb/FORK_NOTES.md

## Branch, builds and core

- The work is on `scene-menu`; `main` is upstream's.
- `Cargo.toml` takes the Ruffle crates from `VlasenkoMykola/Ruffle_fork`, branch
  `claude/relaxed-gauss-ymscnb`; `Cargo.lock` pins the commit. After core changes there,
  run `cargo update -p ruffle_core` here and commit the lock file.
- `Build` and `Lint` run when dispatched (`workflow_dispatch` on `scene-menu`). After the
  APKs build and the emulator tests pass, `Build` calls `publish_build.yml`, which replaces
  the `latest-build` pre-release, whose notes name the commit.
- Lint fails on missing translations: every string in `values/strings.xml` needs one in
  `values-zh-rCN/strings.xml` (which has CRLF line endings), or `translatable="false"`.

## Scene menus

- Buttons in `app/src/main/res/layout/keyboard.xml`: `button_kb` (⌨, shows and hides the
  keyboard, which starts hidden), `button_cm` (▤, the context menu), `button_scenes` (🎬,
  the scene menu).
- ▤ has Ruffle's items, then Scenes and Frame labels submenus and "Go to frame…"; 🎬 has
  the scenes at its top, then Frame labels and "Go to frame…". Both are built by
  `PlayerActivity.TimelineItems`, from entries that Rust sends as
  `"<scene|label|game> <enabled> <checked> <frame> <caption>"`, where the frame of a game
  scene is its index in the game's list. Game scenes show as "1. mainmenu", "2. screen1"...
- Kotlin → Rust (JNI): `requestContextMenu`, `requestSceneMenu`, `gotoRootFrame(frame)`,
  `gotoGameScene(index)`, which send `RuffleEvent`s to the event loop in `src/lib.rs`.
  Rust → Kotlin: `showContextMenu(items, timeline, currentFrame, framesLoaded, totalFrames)`
  and `showSceneMenu(timeline, currentFrame, framesLoaded, totalFrames)`, through method IDs
  in `src/java.rs`.

## Folders and storage

- "Select a folder" opens a document tree; "Select a SWF" opens the file and, when Ruffle
  can read files directly, its folder too, so the movie can load the files next to it.
- The app asks for all-files access (`MANAGE_EXTERNAL_STORAGE`, or `READ_EXTERNAL_STORAGE`
  up to Android 10) once per run. `StorageAccess.fileFor()` maps picker URIs (external
  storage, downloads, media) to files.
- Movies in a folder get `file:///ruffle-folder/...` URLs (`src/navigator.rs`), and the
  navigator opens those through `FolderAccess` (`openFolderFile` over JNI, which hands a
  file descriptor to Rust), matching names ignoring case, since many games were made on
  Windows.

## Tests

Emulator tests in `app/src/androidTest/java/rs/ruffle`: `SceneMenu` (scenes and labels,
with Ruffle's `scenes.swf`), `GameSceneMenu` (game scenes from ▤ and from 🎬, with
`game_scenes.swf`), `FolderContent` (folders, with `folder_test.swf`). The generators of
the fork's test movies are in `tools/test-swfs`.
