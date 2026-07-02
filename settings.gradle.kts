// =========================================================================
// HardwareDash (Gadget) — Gradle settings
// =========================================================================
//
// Phase 0 / Batch 0: module graph for the modular monorepo refactor.
//
// Layout invariants enforced here:
//   * `:app` is the single application module that aggregates feature/*.
//     It uses the Kotlin-DSL `app/build.gradle.kts` and the new applicationId
//     (dev.ranzlappen.gadget[.rooted]). The Kotlin `namespace` stays
//     `com.gadget` until the last legacy package migrates out.
//   * `core/*` holds reusable infrastructure (data, ui, domain, hardware …).
//   * `feature/*` holds one user-facing capability per module. Rooted-only
//     capability surface lives in a sibling `feature/<name>-rooted/` module
//     that only the rooted flavor of `:app` pulls in (via
//     `rootedImplementation`). `:app`'s own dependency list never names a
//     `*-rooted` module in plain `implementation`, so the standard APK is
//     physically incapable of compiling against root code.
//   * `:benchmark` is the macrobenchmark host. Wired up properly in a later
//     batch; Batch 0 ships only the skeleton.
//   * `:lsposed-module` (existing) is only included when
//     `-PenableLsposedModule=true` is set. Standard CI does not; rooted CI
//     does.
//   * `build-logic/` (composite build) hosts the convention plugins
//     (`gadget.android.library`, `gadget.android.feature`, …) every module
//     applies by id.

pluginManagement {
    // Composite-build hook for build-logic/. build-logic/ ships the convention
    // plugins (gadget.android.application, gadget.android.library,
    // gadget.android.library.compose, gadget.android.application.compose,
    // gadget.android.feature, gadget.android.hilt, gadget.android.room,
    // gadget.jvm.library). Module build files apply them by id, e.g.
    // `plugins { id("gadget.android.feature") }`.
    includeBuild("build-logic")
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
        // mavenLocal() is checked first so that CI-built copies of the
        // JitPack libraries (usb-serial-for-android, libsu) are found
        // before the remote JitPack repo is consulted. JitPack returns
        // host_not_allowed on GitHub-hosted runner IPs; the
        // .github/actions/seed-jitpack-cache composite action builds
        // them from source and installs them here on cache miss.
        mavenLocal()
        google()
        mavenCentral()
        // usb-serial-for-android is published only on JitPack. The
        // includeGroup lockdown keeps every other transitive dep going
        // through Maven Central.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroupByRegex("com\\.github\\..*") }
        }
        // Xposed-API maven (only when the LSPosed sub-module is opted in).
        if (providers.gradleProperty("enableLsposedModule").orNull == "true") {
            maven { url = uri("https://api.xposed.info/") }
        }
    }
}

rootProject.name = "Gadget"

// -------------------------------------------------------------------------
// :app — application module (still uses legacy app/build.gradle Groovy
// script; migration to Kotlin DSL is a later batch).
// -------------------------------------------------------------------------
include(":app")

// -------------------------------------------------------------------------
// :core — reusable infrastructure
// -------------------------------------------------------------------------
include(
    ":core:common",
    ":core:designsystem",
    ":core:ui",
    ":core:model",
    ":core:data",
    ":core:monitoring",
    ":core:widgetkit",
    ":core:notifications",
    ":core:datastore",
    ":core:domain",
    ":core:navigation",
    ":core:permissions",
    ":core:root",
    ":core:surfaces",
    ":core:automation",
    ":core:hardware",
    ":core:testing",
)

// -------------------------------------------------------------------------
// :feature — one user-facing capability per module.
//
// `<name>-rooted` modules are sibling modules carrying root-only capability
// surface for the same feature. They are pulled in by the rooted flavor of
// `:app` only — see `rootedImplementation` wiring in a later batch.
// -------------------------------------------------------------------------
include(
    ":feature:automation-ui",
    ":feature:settings",
    ":feature:diagnostics",
    ":feature:diagnostics-rooted",
    ":feature:manual",
    ":feature:sensors",
    ":feature:actuators",
    ":feature:battery",
    ":feature:audio",
    ":feature:camera",
    ":feature:torch",
    ":feature:torch-rooted",
    ":feature:torch-standard",
    ":feature:vibration",
    ":feature:vibration-rooted",
    ":feature:vibration-standard",
    ":feature:gps",
    ":feature:motion",
    ":feature:ambient",
    ":feature:radios-wifi",
    ":feature:radios-bt",
    ":feature:radios-bt-rooted",
    ":feature:radios-nfc",
    ":feature:radios-subghz",
    ":feature:radios-ir",
    ":feature:flipper",
    ":feature:flipper-rooted",
    ":feature:storage",
    ":feature:storage-rooted",
    ":feature:apps",
    ":feature:apps-rooted",
    ":feature:lock",
    ":feature:lock-rooted",
    ":feature:bugreport",
    ":feature:bugreport-rooted",
    ":feature:dashboard",
    ":feature:youtubedownloader",
)

// -------------------------------------------------------------------------
// :benchmark — macrobenchmark host (skeleton only in Batch 0).
// -------------------------------------------------------------------------
include(":benchmark")

// -------------------------------------------------------------------------
// :lsposed-module — bundled Xposed module. Excluded unless the build
// explicitly opts in via -PenableLsposedModule=true. Standard CI does not
// set this; rooted CI does.
// -------------------------------------------------------------------------
if (providers.gradleProperty("enableLsposedModule").orNull == "true") {
    include(":lsposed-module")
}
