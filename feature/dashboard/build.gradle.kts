// :feature:dashboard — home screen.
//
// Adaptive grid of glassmorphic DashCard tiles showing live system
// readouts. Phase 1 ships hardcoded mock data; Phase 2 wires the
// hardware registry from :core:hardware via a Hilt-injected
// ViewModel.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.dashboard"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard,
    // ScreenHeader, SectionHeader, SparklineChart, Glass tokens).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination so the dashboard
    // can request top-level navigation through its onNavigate callback.
    implementation(project(":core:navigation"))
}
