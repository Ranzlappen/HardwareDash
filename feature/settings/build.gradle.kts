// :feature:settings — user-facing preferences.
//
// Phase 2 / Batch 1 ships v1: About + Appearance + Accessibility
// sections backed by :core:datastore. Heavier sections (Backup,
// Flipper, Keep-Alive, Metric logging) ship in dedicated batches
// once their underlying managers are ported from legacy-main.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.settings"

    // BuildConfig is needed so the About card can read VERSION_NAME +
    // VERSION_CODE + flavor info at compose time.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard,
    // CompactCard, the design-system component library).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination + the
    // NavGraphBuilder.settingsScreen() extension target.
    implementation(project(":core:navigation"))
    // :core:datastore exposes UserPreferencesRepository.
    implementation(project(":core:datastore"))
}
