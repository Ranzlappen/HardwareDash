// :core:datastore — persistence layer.
//
// Two surfaces:
//   1. UserPreferencesRepository — app-wide singleton settings (theme,
//      accessibility, default strobe rate, etc.). Backed by a single
//      `user_preferences` Preferences DataStore file.
//   2. FeaturePreferences<T> — generic per-feature collection abstraction.
//      Each feature gets its own Preferences DataStore file (e.g.
//      `torch_widgets`, `vibration_patterns`) backing a Map<Int, T>
//      with kotlinx.serialization JSON values. Provided via
//      FeaturePreferencesFactory so feature modules don't reinvent the
//      DataStore plumbing.
//
// Phase 2 / Batch 1.1 introduces FeaturePreferences<T> as the future-
// proof basis for any feature that needs to persist a small collection
// of structured records keyed by Int (e.g. AppWidget IDs). Torch widgets
// are the first consumer; future modules (Vibration patterns, Sound
// presets, etc.) plug into the same factory.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.core.datastore"
}

dependencies {
    // `api` because UserPreferencesRepository's Flow<UserPreferences>
    // surfaces the data class to every consumer that injects it; the
    // DataStore Preferences runtime is also part of that public surface.
    api(libs.androidx.datastore.preferences)
    // `api` because FeaturePreferences<T>'s constructor takes a
    // KSerializer<T> from kotlinx.serialization; downstream feature
    // modules need the same artefact on their classpath to declare
    // @Serializable data classes that interop with the factory.
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
