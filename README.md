This is a native Android application for https://ruffle.rs.
It is in a very early stage.

# Prerequisites

Install Android Sudio with at least the Platform SDK (e.g. 28) and the NDK Tools (at least r24 needed due to https://github.com/rust-windowing/android-ndk-rs/issues/255).

Also:

`cargo install cargo-apk`

`rustup target add aarch64-linux-android`

# Build Steps

NOTE: First a sacrificial APK is built, then the native library it produces is used to build the proper APK.

Substitute the appropriate locations and ndk version in the variables set for the `cargo-apk` command.

```bash
cd native
ANDROID_SDK_ROOT=$HOME/Android/Sdk/ ANDROID_NDK_ROOT=$HOME/Android/Sdk/ndk/24.0.8215888/ cargo apk build --release

cp -r target/release/apk/lib/arm64-v8a ../app/ruffle/src/main/jniLibs/

cd ../app
./gradlew build
```

The final APK should be at:

`app/ruffle/build/outputs/apk/release/ruffle-release-unsigned.apk`

After the first step, simply opening the `app` project in Android Studio for development also works.

---

# TODO

In no particular order:

- [ ] Ability to show the built-in virtual keyboard (softinput), for keyboard input
- [ ] Own custom keyboard overlay, maybe even per-content configs
- [ ] Navigator backend (fetch, open browser)
- [ ] Error/panic handling
- [ ] Logging?
- [ ] Ui backend (context menu)
- [ ] Unglitchify rendering: letterbox the content properly
- [ ] Proper storage backend?
- [ ] Cross-platform build instructions?
- [ ] Resolve design glitches/styling/theming
- [ ] Unglitchify audio volume (buttons unresponsive?)
  - pending: https://github.com/rust-windowing/winit/pull/1919
- [ ] Support for x86(_64) tablets?
- [ ] Sign the APK, then maybe publish to various app stores, maybe automatically?
- [ ] Consider not building the intermediate .apk just for the shared libraries
- [ ] Simplify build process (hook cargo-apk into gradle, drop cargo-apk?)
- [ ] Add ability to Open SWF by entered/pasted URL (or even directly from clipboard)
- [ ] Bundle demo animations/games
- [ ] Add ability to load content from well known online collections? (well maybe not z0r... unless?)
- [ ] History, favorites, other flair...?
- [ ] Clean up ~everything

### DONE:

- [X] Touch/mouse input
- [X] Keyboard input: only with physical keyboard connected or through `scrcpy`
  - This was needed: https://github.com/rust-windowing/winit/pull/2226
- [X] Split into a separate repo
- [X] Unglitchify rendering: scale and center the content properly
- [X] Add building this to CI, at least to the release workflow
  - This repo has its own CI setup, which builds APKs
- [X] Somehow filter files to be picked to .swf
  - How well this works depends on the file picker, but it "should work most of the time"
- [ ] Register Ruffle to open .swf files
  - How well this works depends on the application opening the file, but it "should work most of the time"
- [X] Figure out why videos are not playing (could be a seeking issue)
  - The video decoder features weren't enabled on `ruffle_core`...
- [ ] Support for 32-bit ARM phones
  - Untested, but should work in theory
- [ ] Unbreak the regular build on CI
  - No longer relevant after the repo split
- [ ] Clean up commit history of the branch
  - No longer relevant after the repo split