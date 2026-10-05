pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For the Kotlin component of the `rustls-platform-verifier` Rust crate.
        maven {
            url = uri(
                "https://github.com/rustls/rustls-platform-verifier/raw/" +
                    "maven-archive/android-release-support/maven/"
            )
            content {
                includeGroup("org.rustls")
            }
        }
    }
}

rootProject.name = "Ruffle"
include(":app")
