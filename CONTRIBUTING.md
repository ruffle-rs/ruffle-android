# Contributing to Ruffle Android
🎉 Thanks for your interest in Ruffle! Contributions of all kinds are welcome.

<!-- TOC -->
* [Contributing to Ruffle Android](#contributing-to-ruffle-android)
  * [Building from source](#building-from-source)
    * [Requirements](#requirements)
    * [Building from Command Line](#building-from-command-line)
    * [Building from Android Studio](#building-from-android-studio)
    * [Development tips](#development-tips)
      * [Limit your targets](#limit-your-targets)
      * [Use emulators!](#use-emulators)
    * [Troubleshooting](#troubleshooting)
      * ["error: Error detecting NDK version for path"](#error-error-detecting-ndk-version-for-path)
  * [Code guidelines](#code-guidelines)
    * [Rust](#rust)
    * [Kotlin](#kotlin)
<!-- TOC -->

## Building from source
### Requirements
Before you can build the app from source, you'll need to grab a few things.

- Install Android Studio with at least the Platform SDK (e.g. version 34) and the NDK Tools (e.g. version 26).
- Install jdk 17 (potentially included with Android Studio)
- Install [rust](https://rustup.rs/)
- `cargo install cargo-ndk`
- `rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android`
- Set the `ANDROID_NDK_ROOT` environment variable to the location of the versioned ndk folder - for example `ANDROID_NDK_ROOT=$HOME/Android/Sdk/ndk/24.0.8215888/`

### Building from Command Line
If you're completely using the command line and bypassing Android Studio, make sure that the `local.properties`
file exists at the repository root folder. It can be empty.

To build the apks run `./gradlew assembleDebug`. You will find the resulting APKs in `app/build/outputs/apk/debug/`.

### Building from Android Studio
Just open the repository root directory in Android Studio, it should automatically set up all the right things.

### Development tips
#### Limit your targets
To speed up iteration, you can tell gradle to only build the rust project for one specific target - the emulator/device you're using.
To do this, add `ndkTargets=arm64` (for example) to your `local.properties`. To specify more than one target, separate them with a space.
The default value is all 4 targets, so that's cutting the build time by 75%!

#### Use emulators!
An Android device is **not** required to build or test Ruffle. Android Studio defaults to deploying and testing on an emulator.

Feel free to install other emulators to help test different form factors or older versions of Android OS.

### Use android target
Set the target of your favourite tools (such as Rust Rover or Rust Analyzer) to `aarch64-linux-android` (or similar)
to stop any errors from it trying to compile Android code for an unsupported platform.
If that isn't enough, you may also need to set the `TARGET_CC`, `TARGET_CXX`, and `TARGET_AR` environment variables to the
full paths of the (sometimes API level-specific) `clang`, `clang++`, and `llvm-ar` binaries from the NDK, respectively.

### Troubleshooting

#### "error: Error detecting NDK version for path"
This means your `ANDROID_NDK_ROOT` environment variable is missing or incorrect.

## Code guidelines
We have strict guidelines about trivial code quality matters (formatting and easily checkable lints).
These are enforced by Github Actions, but you are encouraged to run these locally to avoid having to iterate *after* a PR has been opened.

### Rust
Ruffle is built using the latest stable version of the Rust compiler. Nightly and unstable features should be avoided.
The Rust code in Ruffle strives to be idiomatic. The Rust compiler should emit no warnings when building the project.
Additionally, all code should be formatted using [`rustfmt`](https://github.com/rust-lang/rustfmt) and linted using [`clippy`](https://github.com/rust-lang/rust-clippy).
You can install these tools using `rustup`:

```sh
rustup component add rustfmt
rustup component add clippy
```

You can auto-format your changes with `rustfmt`

```sh
cargo fmt --all
```

And you can run the clippy lints:

```sh
cargo ndk clippy --all --tests
```

Specific warnings and clippy lints can be allowed when appropriate using attributes, such as:

```rs
#[allow(clippy::float_cmp)]
```

### Kotlin
We try to use Kotlin where possible, no Java. Additionally, we try to use best practices and maintain readable, idiomatic code.

To automatically format all Kotlin code, run:

```sh
./gradlew ktlintFormat
```

And to automatically lint the code, use:

```sh
./gradlew lint
```
