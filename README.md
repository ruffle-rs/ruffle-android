This is a native Android application for [Ruffle](https://ruffle.rs).

It is in a very early stage.

# Prebuilt APKs

The latest (successful) Actions run [(here)](https://github.com/torokati44/ruffle-android/actions?query=actor%3Atorokati44+branch%3Amain+is%3Asuccess) should have a debug and a release `.apk` uploaded as artifacts.

You can try this app by downloading and installing one of those.

# Build Prerequisites

Install Android Sudio with at least the Platform SDK (e.g. 28) and the NDK Tools (at least r24 needed due to https://github.com/rust-windowing/android-ndk-rs/issues/255).

Also:

`cargo install cargo-apk`

`rustup target add aarch64-linux-android armv7-linux-androideabi`

# Build Steps

NOTE: First a sacrificial APK is built, then the native library it produces is used to build the proper APK.

Substitute the appropriate locations and NDK version in the variables set for the `cargo-apk` command.

```bash
cd native
# don't specify a `--target` here, as that changes the directory structure
ANDROID_SDK_ROOT=$HOME/Android/Sdk/ ANDROID_NDK_ROOT=$HOME/Android/Sdk/ndk/24.0.8215888/ cargo apk build --release

mkdir ../app/ruffle/src/main/jniLibs
cp -r target/release/apk/lib/* ../app/ruffle/src/main/jniLibs/

cd ../app
./gradlew assembleDebug # the "release" version requires a keyfile
```

The final APK should be at:

`app/ruffle/build/outputs/apk/debug/ruffle-debug.apk`

After the first step, simply opening the `app` project in Android Studio for development also works.

---

# TODO

In no particular order:

- [ ] Ability to show the built-in virtual keyboard (softinput), for keyboard input
- [ ] Controller/Gamepad input?
  - Mapped to key presses and/or virtual mouse pointer
- [ ] Own custom keyboard overlay, maybe even per-content configs
- [ ] Navigator backend (fetch, open browser)
- [ ] Error/panic handling
- [ ] Loading "animation" (spinner)
- [ ] Logging?
- [ ] Alternative audio backend (OpenSL ES) for Android < 8
- [ ] Ui backend (context menu)
- [ ] Proper storage backend?
- [ ] Cross-platform build instructions?
- [ ] Resolve design glitches/styling/theming (immersive mode, window insets for holes/notches/corners)
- [ ] Unglitchify audio volume (buttons unresponsive?)
  - pending: https://github.com/rust-windowing/winit/pull/1919
- [ ] Support for x86(_64) tablets?
- [ ] Publish to various app stores, maybe automatically?
- [ ] Consider not building the intermediate .apk just for the shared libraries
- [ ] Simplify build process (hook cargo-apk into gradle, drop cargo-apk?)
- [ ] Bundle demo animations/games
- [ ] Add ability to load content from well known online collections? (well maybe not z0r... unless?)
- [ ] History, favorites, other flair...?
- [ ] Clean up ~everything

### DONE:

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
- [X] Somehow filter files to be picked to .swf
  - How well this works depends on the file picker, but it "should work most of the time"
- [ ] Register Ruffle to open .swf files
  - How well this works depends on the application opening the file, but it "should work most of the time"
- [X] Figure out why videos are not playing (could be a seeking issue)
  - The video decoder features weren't enabled on `ruffle_core`...
- [X] Sign the APK
  - Using a very simple key for now, with just my name in it
- [X] Support for 32-bit ARM phones
  - Untested, but should work in theory
- [ ] Unbreak the regular build on CI
  - No longer relevant after the repo split
- [ ] Clean up commit history of the branch
  - No longer relevant after the repo split