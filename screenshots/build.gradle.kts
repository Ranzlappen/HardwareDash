// =========================================================================
// :screenshots — Roborazzi Compose-preview aggregator (TEST-ONLY).
// =========================================================================
//
// This module ships nothing to users. It exists so Roborazzi's Compose
// preview scanner can render *every* @Preview in the app in a single JVM
// (Robolectric) pass — no emulator — feeding the auto-generated app-preview
// gallery (`scripts/build_preview_gallery.py` +
// `.github/workflows/app-preview.yml`).
//
// It is the ONE sanctioned place that depends broadly on the feature graph:
// the scanner discovers a screen's preview only if that screen's class is on
// this module's classpath. To keep the CLAUDE.md flavor-separation invariant
// intact, rooted-only feature modules are added *only* when
// `-PenableRootedPreviews=true` (mirrors the `-PenableLsposedModule` gate),
// so the default standard render set never compiles against root code.
//
// `generateComposePreviewRobolectricTests` synthesises one Robolectric test
// per @Preview and honours each preview's uiMode (light/dark), fontScale,
// device / widthDp — so the existing @GadgetPreviewLightDark /
// @GadgetPreviewLargeFont / @GadgetPreviewRtl / @GadgetPreviewSizeClasses
// matrix renders as separate PNGs with zero extra code.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "dev.ranzlappen.gadget.screenshots"

    testOptions {
        unitTests {
            // Robolectric needs merged Android resources to inflate the
            // themed Compose content (the design-system colours/typography
            // live in resources pulled from :core:designsystem).
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

roborazzi {
    generateComposePreviewRobolectricTests {
        enable = true
        // Scan the whole app namespace. Every feature/core preview lives
        // under dev.ranzlappen.gadget.**, so this one root covers them all.
        packages = listOf("dev.ranzlappen.gadget")
    }
}

dependencies {
    // ---- Design system + shared UI (render targets live downstream) ----
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))

    // ---- Standard-flavor feature surfaces (the "entire app") -----------
    // Base feature modules hold the screens + their @Preview functions.
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:torch"))
    implementation(project(":feature:torch-standard"))
    implementation(project(":feature:vibration"))
    implementation(project(":feature:vibration-standard"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:sensors"))
    implementation(project(":feature:actuators"))
    implementation(project(":feature:battery"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:camera"))
    implementation(project(":feature:gps"))
    implementation(project(":feature:motion"))
    implementation(project(":feature:ambient"))
    implementation(project(":feature:radios-wifi"))
    implementation(project(":feature:radios-bt"))
    implementation(project(":feature:radios-nfc"))
    implementation(project(":feature:radios-subghz"))
    implementation(project(":feature:radios-ir"))
    implementation(project(":feature:flipper"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:lock"))
    implementation(project(":feature:bugreport"))
    implementation(project(":feature:diagnostics"))
    implementation(project(":feature:manual"))
    implementation(project(":feature:automation-ui"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:youtubedownloader"))

    // ---- Rooted-only surfaces (opt-in; never on the standard set) ------
    // Gated behind -PenableRootedPreviews so the default render never
    // compiles against root code (CLAUDE.md flavor-separation invariant).
    if (providers.gradleProperty("enableRootedPreviews").orNull == "true") {
        implementation(project(":feature:torch-rooted"))
        implementation(project(":feature:vibration-rooted"))
        implementation(project(":feature:apps-rooted"))
        implementation(project(":feature:radios-wifi-rooted"))
        implementation(project(":feature:radios-bt-rooted"))
        implementation(project(":feature:radios-nfc-rooted"))
        implementation(project(":feature:flipper-rooted"))
        implementation(project(":feature:storage-rooted"))
        implementation(project(":feature:lock-rooted"))
        implementation(project(":feature:bugreport-rooted"))
        implementation(project(":feature:diagnostics-rooted"))
    }

    // ---- Roborazzi / Robolectric preview-render stack (test-only) ------
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    // Provides the ComposePreviewTester wiring that
    // generateComposePreviewRobolectricTests targets; transitively brings
    // the ComposablePreviewScanner.
    testImplementation(libs.roborazzi.compose.preview.scanner.support)
    testImplementation(libs.composable.preview.scanner.android)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    // Compose test harness used by the generated capture tests.
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)
}
