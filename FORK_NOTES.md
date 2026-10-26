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
- `Build` and `Lint` run when dispatched (`workflow_dispatch` on `scene-menu`). `Build`
  uploads each APK as its own unzipped artifact (`app-arm64-v8a-release.apk`...), since the
  user's connection is slow and they only need the arm64 one. After the emulator tests
  pass, it calls `publish_build.yml`, which uploads the APKs to the `latest-build`
  pre-release, whose notes name the commit:
  https://github.com/VlasenkoMykola/ruffle-android-with-sceneselect/releases/download/latest-build/app-arm64-v8a-release.apk.
  The release is updated in place: deleting it with its tag and making it again (as the
  workflow first did) left it a hidden draft, with every download link failing.
- Lint fails on missing translations: every string in `values/strings.xml` needs one in
  `values-zh-rCN/strings.xml` (which has CRLF line endings), or `translatable="false"`.

## Scene menus

- Buttons in `app/src/main/res/layout/keyboard.xml`: `button_kb` (⌨, shows and hides the
  keyboard, which starts hidden), `button_cm` (▤, the context menu), `button_scenes` (🎬,
  the scene menu).
- The three buttons are in `toolbar`, as wide as they are. Holding one of them still for
  0.5 s (`MOVE_HOLD_MILLIS`) makes the bar grow a little (`MOVING_SCALE`; dimming it made it
  invisible over the game's surface while dragged) and vibrates, and the whole bar can then
  be dragged
  anywhere on the screen (`PlayerActivity.makeToolbarMovable`); a tap still clicks, and a
  drag without the hold does nothing. Where the bar was left is kept in the `player`
  preferences (`toolbarX`, `toolbarY`), as a share of the room it can move in, so it fits
  either orientation.
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

## Keeping the game when leaving the app

`MainActivity` is `singleTop`: with upstream's `singleTask`, coming back to the app with its
launcher icon brought back the first screen and closed everything above it, the game
included. `PlayerActivity` handles more configuration changes itself (window and screen
size, screen layout, density, keyboard, navigation, UI mode, locale, font scale), as
Android would recreate it for them, restarting the game. The Rust side keeps the player
when the window goes (`TerminateWindow` pauses it, `InitWindow` gives it the new surface).
If Android kills the app's process in the background to free memory, the game still starts
over when it's opened again.

## Tests

Emulator tests in `app/src/androidTest/java/rs/ruffle`: `SceneMenu` (scenes and labels,
with Ruffle's `scenes.swf`), `GameSceneMenu` (game scenes from ▤ and from 🎬, with
`game_scenes.swf`), `FolderContent` (folders, with `folder_test.swf`), `LeaveAndReturn`
(a game opened from the first screen survives leaving the app and coming back with its
launcher icon), `MovableButtons` (dragging the buttons, which clears their position
after). The generators of
the fork's test movies are in `tools/test-swfs`. The API 26 emulator once crashed the test
process during `FolderContent` (run 39646519827, for a core change it doesn't reach); the
re-run of the failed jobs passed.
