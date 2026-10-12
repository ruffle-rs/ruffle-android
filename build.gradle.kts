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
    // Vendored code is kept as-is, so it isn't linted.
    if (projectDir.startsWith(rootDir.resolve("vendor"))) return@allprojects
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
}

ktlint {
    android.set(true)
}
