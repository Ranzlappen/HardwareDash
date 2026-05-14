// :feature:torch — flashlight feature surface.
//
// Phase 2 / Batch 1 shipped v1: standard-flavor Camera2 controller +
// TorchScreen + QS tile + home-screen toggle widget + strobe widget
// (foreground service).
//
// Phase 2 / Batch 1.1 polished the migration into a flawless precedent
// for every future feature port:
//   * Dynamic flashlight/strobe widget creation from inside the app
//     via AppWidgetManager.requestPinAppWidget.
//   * Per-widget persisted config (TorchWidgetConfig) backed by the
//     generic :core:datastore FeaturePreferences<T> abstraction.
//   * Persistent strobe-rate slider (UserPreferences.defaultStrobeRateHz).
//
// Rooted extras (DutyCycleStrobe / MultiLedOrchestrator /
// ThermalOverrideController) ship as a sibling `:feature:torch-rooted`
// module after the root infrastructure ports — tracked at
// https://github.com/Ranzlappen/HardwareDash/issues/94.
//
// See docs/migration-guide.md for the eight-step recipe this module is
// the worked example of.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.torch"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard,
    // GadgetFab, the design-system component library).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination.Torch + the
    // NavGraphBuilder.torchScreen() extension target.
    implementation(project(":core:navigation"))
    // :core:datastore exposes UserPreferencesRepository (persistent
    // strobe-rate slider) + FeaturePreferencesFactory
    // (per-widget TorchWidgetConfig persistence).
    implementation(project(":core:datastore"))
    // androidx.core for NotificationCompat (StrobeService's
    // foreground notification builder). Explicit even though
    // lifecycle-runtime-ktx pulls it in transitively — surfaces
    // the dependency at the place that consumes it.
    implementation(libs.androidx.core.ktx)
    // kotlinx.serialization JSON — TorchWidgetConfig is @Serializable
    // and rides through FeaturePreferences<T>'s JSON encoder.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
