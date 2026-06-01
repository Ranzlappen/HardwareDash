// :feature:vibration — haptic / vibration motor feature surface.
//
// The second end-to-end modular migration (after :feature:torch), built to
// the Module Authoring Contract in CLAUDE.md. Standard-flavor haptics
// (VibratorManager / VibrationEffect: predefined effects, amplitude control,
// a draw-canvas pattern builder) + monitoring + automation + a 4-widget
// surface, with the privileged extreme-tier capabilities (extreme amplitude,
// direct PWM, dual-actuator, sustained rumble) shipping in the sibling
// :feature:vibration-rooted module.
//
// Mirrors :feature:torch 1:1 — see docs/migration-guide.md for the recipe and
// docs/refactor-2026/vibration-migration.md for this migration's notes.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.vibration"

    defaultConfig {
        // Keep rules for this module's @Serializable types — merged into
        // :app's release R8 run so minified builds don't strip the
        // generated serializers. See consumer-rules.pro.
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard, GadgetSlider,
    // GadgetCircleControl, the design-system component library).
    implementation(project(":core:ui"))
    // :core:widgetkit — the reusable home-screen widget framework (pin flow,
    // BaseGadgetWidgetProvider, WidgetConfigStore, appearance renderer).
    implementation(project(":core:widgetkit"))
    // :core:monitoring — the monitoring container + MetricSource seam.
    implementation(project(":core:monitoring"))
    // :core:data — the monitor widget reads persisted sample history via
    // MonitorSampleRepository (the sanctioned ":core:data repositories" path).
    implementation(project(":core:data"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    implementation(project(":core:automation"))
    // :core:navigation surfaces GadgetDestination.Vibration + the
    // NavGraphBuilder.vibrationScreen() extension target.
    implementation(project(":core:navigation"))
    // :core:datastore exposes FeaturePreferencesFactory (per-widget config,
    // saved patterns, rooted-tool settings persistence).
    implementation(project(":core:datastore"))
    // androidx.core for NotificationCompat (VibrationPlaybackService's
    // foreground notification builder).
    implementation(libs.androidx.core.ktx)
    // kotlinx.serialization JSON — VibrationWidgetConfig / VibrationPattern /
    // VibrationRootToolsConfig are @Serializable and ride through
    // FeaturePreferences<T>'s JSON encoder.
    implementation(libs.kotlinx.serialization.json)
    // ExifInterface — applies a picked custom-icon image's EXIF orientation
    // so gallery photos don't render sideways on the widget.
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
