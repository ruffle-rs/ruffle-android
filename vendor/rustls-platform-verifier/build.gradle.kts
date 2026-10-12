// The Kotlin component of the `rustls-platform-verifier` Rust crate, vendored from
// https://github.com/rustls/rustls-platform-verifier/tree/main/android so that it
// doesn't have to be fetched from their Maven repository. See README.md.

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

// The Rust side calls into this code via JNI, so they must be kept in sync.
// This must match the version of the `rustls-platform-verifier-android` crate
// in Cargo.lock, which is checked below.
val vendoredVersion = "0.2.0"

android {
    namespace = "org.rustls.platformverifier"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        // Only used by upstream's own tests.
        buildConfigField("boolean", "TEST", "false")
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
    val vendorDir = projectDir.relativeTo(rootDir)
    inputs.file(lockFile)
    doLast {
        val lines = lockFile.asFile.readLines()
        val nameIdx = lines.indexOfFirst { it.trim() == "name = \"$crateName\"" }
        val lockedVersion = lines.drop(nameIdx + 1)
            .firstOrNull { it.trimStart().startsWith("version = ") }
            ?.substringAfter('"')
            ?.substringBefore('"')
            .takeIf { nameIdx >= 0 }
        check(lockedVersion == vendoredVersion) {
            "Cargo.lock has $crateName $lockedVersion, but version $vendoredVersion of its " +
                "Kotlin component is vendored in $vendorDir. See README.md there."
        }
    }
}

tasks.named("preBuild") {
    dependsOn(checkCargoLockVersion)
}
