// :core:datastore — user preferences persistence.
//
// Wraps androidx.datastore (Preferences flavour) with a typed
// UserPreferences data class + Hilt-provided UserPreferencesRepository.
// Phase 2 / Batch 1 introduces the first preference slots
// (dark theme mode, dynamic color, reduced-motion override, reduced-
// transparency, large-text override). Future batches grow this as
// features migrate.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.datastore"
}

dependencies {
    // `api` because UserPreferencesRepository's Flow<UserPreferences>
    // surfaces the data class to every consumer that injects it; the
    // DataStore Preferences runtime is also part of that public surface.
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
