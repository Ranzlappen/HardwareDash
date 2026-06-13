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
    // VERSION_CODE + flavor info at compose time. Library modules
    // don't auto-inherit the :app module's versionName/versionCode,
    // so we re-derive them from the same gradle properties the :app
    // module reads — keeps the displayed version aligned with the
    // installed package version without forcing a cross-module
    // BuildConfig dependency.
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val ciVersionName = providers.gradleProperty("CI_VERSION_NAME").getOrElse("1.0-dev")
        val ciVersionCode = providers.gradleProperty("CI_VERSION_CODE").orNull?.toInt() ?: 1
        buildConfigField("String", "VERSION_NAME", "\"$ciVersionName\"")
        buildConfigField("int", "VERSION_CODE", ciVersionCode.toString())
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
    // :core:monitoring exposes MonitorGlobalPrefs for the monitoring settings card.
    implementation(project(":core:monitoring"))
}
