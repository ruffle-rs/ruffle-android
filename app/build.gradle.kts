@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.github.willir.rust.CargoNdkBuildTask
import org.jetbrains.kotlin.konan.properties.hasProperty
import org.jetbrains.kotlin.konan.properties.propertyList

val localProperties = gradleLocalProperties(rootDir, providers)
val abiFilterList = ((localProperties["ABI_FILTERS"] ?: properties["ABI_FILTERS"]) as? String)
    ?.split(';')
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

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
        versionCode = 250721
        versionName = "0.250721"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            if (abiFilterList == null) {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
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
            if (abiFilterList != null && abiFilterList.isNotEmpty()) {
                include(*abiFilterList.toTypedArray())
            } else {
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

                // Specifies that we also want to generate a universal APK that includes all ABIs.
                isUniversalApk = true
            }
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val name = output.filters.find { it.filterType == ABI }?.identifier
            val abiCode = abiCodes[name] ?: 0
            output.versionCode.set(output.versionCode.get() * 10 + abiCode)
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

    if (localProperties.hasProperty("ndkTargets")) {
        targets = ArrayList(localProperties.propertyList("ndkTargets"))
    }
}
