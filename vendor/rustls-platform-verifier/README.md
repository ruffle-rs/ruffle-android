# rustls-platform-verifier (Kotlin component)

This is the Android/Kotlin component of the
[`rustls-platform-verifier`](https://github.com/rustls/rustls-platform-verifier)
Rust crate, which reqwest uses to verify server certificates against Android's
trust store. It is vendored here (instead of being fetched from upstream's own
Maven repository) so the app can be built from source using only trusted
repositories, e.g. by F-Droid.

The files under `src/main` and the licenses are unmodified copies from upstream's
`android/rustls-platform-verifier` directory at commit
`252e25161a91af476cbea620e29d277592d27ec7` (tag `v/0.7.1`), which is the one
`rustls-platform-verifier-android` 0.2.0 was published from. Upstream's tests
are omitted, and `build.gradle.kts` is our own.

## Updating

When the `rustls-platform-verifier-android` crate is updated in `Cargo.lock`, the
build fails until this copy is updated too:

1. Find the upstream commit the new crate version was published from, in the
   `.cargo_vcs_info.json` file of the crate (e.g. on docs.rs, under "Source").
2. Replace `src/main` and the licenses with the ones from that commit.
3. Update `vendoredVersion` in `build.gradle.kts`, and the commit above.
