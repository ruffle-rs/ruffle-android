This is a native Android application for [Ruffle](https://ruffle.rs).

It is in a very early stage.

# Prebuilt APKs

The latest release [(here)](https://github.com/torokati44/ruffle-android/releases) should have a few `.apk` files uploaded as assets.

You can try this app by downloading and installing one of those.

- **For the vast majority of modern phones, tablets, single board computers, and small game consoles, you'll need the `arm64-v8a` version.**

- The `armeabi-v7a` version is for older, 32-bit ARM stuff.

- The `x86_64` version is for some rare Intel/Microsoft tablets and/or for Chromebooks, and/or for running on a PC on Android-x86 or in Waydroid or similar.

- The `x86` version is there mostly just for completeness.

- The `universal` version should work on all 4 of the above architectures, but it's _huge_.

# Building from source

Please see [CONTRIBUTING.md](CONTRIBUTING.md#building-from-source) for details about how to build this repository yourself.

---

# TODO

In no particular order:

- [ ] Ability to show the built-in virtual keyboard (softinput), for text input
- [ ] Controller/Gamepad input?
  - Mapped to key presses and/or virtual mouse pointer
- [ ] Own custom keyboard overlay, maybe even per-content configs
  - Not an overlay, and not per-content, but custom keyboard is there
- [ ] Error/panic handling
- [X] Loading "animation" (spinner)
- [ ] Alternative audio backend (OpenSL ES) for Android < 8
- [ ] Proper storage backend?
- [X] Resolve design glitches/styling/theming (immersive mode, window insets for holes/notches/corners)
- [ ] Publish to various app stores, maybe automatically?
- [X] Bundle demo animations/games
- [ ] Add ability to load content from well known online collections? (well maybe not z0r... unless?)
- [X] History, favorites, other flair...?

### DONE:

- [X] Clean up ~everything
- [X] Cross-platform build instructions?
  - I think gradle should take care of it now
- [X] UI backend (context menu)
  - Context menu works
- [X] Logging?
- [X] Navigator backend (fetch, open browser)
  - Opening links works at least
- [X] Touch/mouse input
- [X] Keyboard input: only with physical keyboard connected or through `scrcpy`
  - This was needed: https://github.com/rust-windowing/winit/pull/2226
- [X] Split into a separate repo
- [X] Add ability to Open SWF by entered/pasted URL (or even directly from clipboard)
  - No direct clipboard open, but easy to paste into the text field...
- [X] Unglitchify rendering: scale, center and letterbox the content properly
- [ ] Ask CPAL/Oboe to open a "media" type output stream instead of a "call" one
  - so the right volume slider controls it, and it uses the loud(er)speaker
  - -> solved by switching to a direct AAudio (ndk-audio) backend
- [X] Add building this to CI, at least to the release workflow
  - This repo has its own CI setup, which builds APKs
- [X] Simplify build process (hook cargo-apk into gradle, drop cargo-apk?)
  - ~cargo-apk is fine, but is only used to detect the SDK/NDK environment and run Cargo in it, and not to build an APK.~
  - actually solved by switching to `cargo-ndk` and the corresponding Gradle plugin
- [X] Somehow filter files to be picked to .swf
  - How well this works depends on the file picker, but it "should work most of the time"
- [X] Unglitchify audio volume (buttons unresponsive?)
  - (pending: https://github.com/rust-windowing/winit/pull/1919)
  - actually solved by switching to GameActivity instead
- [X] Register Ruffle to open .swf files
  - How well this works depends on the application opening the file, but it "should work most of the time"
- [X] Figure out why videos are not playing (could be a seeking issue)
  - The video decoder features weren't enabled on `ruffle_core`...
- [X] Sign the APK
  - Using a very simple key for now, with just my name in it
- [X] Support for 32-bit ARM phones
  - Untested, but should work in theory
- [X] Support for x86(_64) tablets?
  - Sorted out
- [X] Consider not building the intermediate .apk just for the shared libraries
  - Figured out, no intermediate .apk any more, only native libs built
- [ ] Unbreak the regular build on CI
  - No longer relevant after the repo split
- [ ] Clean up commit history of the branch
  - No longer relevant after the repo split
