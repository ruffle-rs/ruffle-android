// The Kotlin component of the `rustls-platform-verifier` Rust crate, built from
// the upstream sources in the vendor/rustls-platform-verifier git submodule, so
// that it doesn't have to be fetched from upstream's own Maven repository.
//
// The Rust side calls into this code via JNI, so they must be kept in sync: the
// submodule must be at the upstream commit that the `rustls-platform-verifier-android`
// crate in Cargo.lock was published from. That commit is in the `.cargo_vcs_info.json`
// file of the crate (e.g. on docs.rs, under "Source"). The crate versions are checked
// below, to catch the submodule not being updated along with Cargo.lock.

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val upstreamDir = rootDir.resolve("vendor/rustls-platform-verifier")
val upstreamSources = upstreamDir.resolve("android/rustls-platform-verifier/src/main")

android {
    namespace = "org.rustls.platformverifier"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        // Only used by upstream's own tests.
        buildConfigField("boolean", "TEST", "false")
    }

    sourceSets {
        named("main") {
            manifest.srcFile(upstreamSources.resolve("AndroidManifest.xml"))
            java.srcDir(upstreamSources.resolve("java"))
            res.srcDir(upstreamSources.resolve("res"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

val checkCargoLockVersion by tasks.registering {
    val crateName = "rustls-platform-verifier-android"
    val lockFile = rootProject.layout.projectDirectory.file("Cargo.lock")
    val crateManifest = upstreamDir.resolve("android-release-support/Cargo.toml")
    val submodulePath = upstreamDir.relativeTo(rootDir)
    inputs.file(lockFile)
    // Not `inputs.file`, so that a missing submodule gets the error message below.
    inputs.files(crateManifest)
    doLast {
        check(crateManifest.exists()) {
            "The $submodulePath submodule is missing, run: git submodule update --init"
        }

        fun versionAfter(lines: List<String>, nameLine: String): String? {
            val nameIdx = lines.indexOfFirst { it.trim() == nameLine }
            if (nameIdx < 0) return null
            return lines.drop(nameIdx + 1)
                .firstOrNull { it.trimStart().startsWith("version = ") }
                ?.substringAfter('"')
                ?.substringBefore('"')
        }

        val nameLine = "name = \"$crateName\""
        val lockedVersion = versionAfter(lockFile.asFile.readLines(), nameLine)
        val vendoredVersion = versionAfter(crateManifest.readLines(), nameLine)
        check(lockedVersion == vendoredVersion) {
            "Cargo.lock has $crateName $lockedVersion, but the $submodulePath " +
                "submodule has $vendoredVersion. See rustls-platform-verifier/build.gradle.kts."
        }
    }
}

tasks.named("preBuild") {
    dependsOn(checkCargoLockVersion)
}
