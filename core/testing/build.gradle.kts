// :core:testing — Compose-aware test helpers shared across module androidTest
// source sets. Other modules consume via:
//
//     dependencies { androidTestImplementation(project(":core:testing")) }
//
// The helpers live in src/main/ (not src/androidTest/) so they ship as
// production code in this module's classpath — that's the conventional
// pattern for cross-module test fixtures.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
}

android {
    namespace = "dev.ranzlappen.gadget.core.testing"
}

dependencies {
    // `api` because GadgetTestTheme wraps content in the real GadgetTheme
    // and the test helpers materialise LocalGadgetTheme / LocalWindowSizeClass
    // values — both types must be visible to consumers transitively.
    api(project(":core:designsystem"))
    api(project(":core:ui"))

    // Compose UI test runtime — `api` so consuming modules don't have to
    // re-declare the dep in their own androidTest configurations. Pairs
    // with the `manifest` artefact below for activity-launch support.
    api(libs.androidx.ui.test.junit4)

    // `debugImplementation`-equivalent for libraries — the test-manifest
    // contributes an empty Activity to the test APK so tests that need
    // composeTestRule.setContent { } can launch one without an explicit
    // host activity.
    debugImplementation(libs.androidx.ui.test.manifest)
}
