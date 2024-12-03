@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.github.willir.rust.CargoNdkBuildTask
import org.jetbrains.kotlin.konan.properties.hasProperty
import org.jetbrains.kotlin.konan.properties.propertyList

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.cargoNdkAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "rs.ruffle"
    compileSdk = 35

    defaultConfig {
        applicationId = "rs.ruffle"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }
    }

    signingConfigs {
        val keyFile = file("androidkey.jks")
        val storePasswordVal = System.getenv("SIGNING_STORE_PASSWORD")
        if (keyFile.exists() && storePasswordVal.isNotEmpty()) {
            create("release") {
                storeFile = keyFile
                storePassword = storePasswordVal
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        prefab = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true

            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

            // Specifies that we also want to generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// On GHA, we prebuild the native libs separately for fasterness,
// and this plugin doesn't recognize them, so would build them again.
if (System.getenv("GITHUB_ACTIONS") != null) {
    tasks.withType<CargoNdkBuildTask> {
        enabled = false
    }
}

cargoNdk {
    module = "."
    apiLevel = 26
    buildType = "release"

    val localProperties = gradleLocalProperties(rootDir, providers)

    if (localProperties.hasProperty("ndkTargets")) {
        targets = ArrayList(localProperties.propertyList("ndkTargets"))
    }
}
