// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.cargoNdkAndroid) apply false
}

allprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            // Vendored code (in git submodules) is kept as-is, so it isn't linted.
            exclude { it.file.startsWith(rootDir.resolve("vendor")) }
        }
    }
}

ktlint {
    android.set(true)
}
