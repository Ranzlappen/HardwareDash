// :feature:torch — flashlight feature surface.
//
// Phase 2 / Batch 1 ships v1: standard-flavor Camera2 controller +
// TorchScreen + QS tile + home-screen toggle widget + strobe widget
// (foreground service). Rooted extras (DutyCycleStrobe,
// MultiLedOrchestrator, ThermalOverrideController) are deferred —
// they need RootCapabilityRegistry / RootSafetyGate to be ported
// from legacy-main first.
//
// See docs/migration-guide.md for the eight-step recipe this
// module is the first worked example of.

plugins {
    id("gadget.android.feature")
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
    // androidx.core for NotificationCompat (StrobeService's
    // foreground notification builder). Explicit even though
    // lifecycle-runtime-ktx pulls it in transitively — surfaces
    // the dependency at the place that consumes it.
    implementation(libs.androidx.core.ktx)
}
