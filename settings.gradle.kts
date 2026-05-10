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
        maven { url = uri("https://jitpack.io") } // usb-serial-for-android
        // Xposed-API maven (only resolved when the LSPosed sub-module is included).
        if (providers.gradleProperty("enableLsposedModule").orNull == "true") {
            maven { url = uri("https://api.xposed.info/") }
        }
    }
}

rootProject.name = "Gadget"
include(":app")

// Bundled LSPosed module — built only when explicitly opted in via Gradle
// property. Standard CI does not set this; rooted CI does. The asset-copy
// task in app/build.gradle is similarly gated on the same property.
if (providers.gradleProperty("enableLsposedModule").orNull == "true") {
    include(":lsposed-module")
}
