// =========================================================================
// build-logic — composite-build settings for the Gadget convention plugins.
// =========================================================================
//
// This file is the *settings* for the build-logic composite build (the
// included build pulled in by the root project's `includeBuild("build-
// logic")` in settings.gradle.kts).
//
// Two responsibilities:
//
//   1. Declare repositories for the convention plugin's own dependency
//      resolution (it depends on AGP, Kotlin, KSP, Room Gradle plugins as
//      compileOnly).
//
//   2. Re-declare the `libs` version catalog by reading the same
//      `gradle/libs.versions.toml` the rest of the build uses. Included
//      builds do not automatically inherit the catalog; without this, the
//      convention plugin source files cannot resolve `libs.android
//      .gradlePlugin` etc.

pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
