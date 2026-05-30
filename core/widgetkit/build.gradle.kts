// :core:widgetkit — reusable home-screen widget framework.
//
// The generic half of the per-feature widget subsystem, extracted so a
// 30-module app doesn't re-hand-roll the pin flow, per-appWidgetId
// persistence, RemoteViews appearance rendering, the icon catalog, and
// toast/notification feedback. Features plug in via a config implementing
// WidgetKitConfig; the kit never depends on a feature module.
//
// Status (Phase 2 / refactor-2026, C1): the value-type family
// (WidgetAppearance, ToggleFeedback, BackgroundMode, IconStyle, IconTint,
// TapBehavior, TapAnimation, WidgetIconSource, WidgetIconKeys) moves here.
// The remaining resource- and Hilt-coupled layer (RemoteViews renderer,
// icon catalog, providers, store, pin-flow bridge) follows in C2–C7.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.core.widgetkit"
}

dependencies {
    // FeaturePreferences<T> + FeaturePreferencesFactory — the
    // DataStore-backed per-feature collection the kit's
    // WidgetConfigStore / PendingWidgetConfigs wrap.
    api(project(":core:datastore"))
    // :core:designsystem — LocalGadgetTheme tokens consumed by the
    // kit's Compose surfaces (WidgetAppearancePreview etc.).
    api(project(":core:designsystem"))
    // :core:notifications — NotificationChannelRegistry the
    // WidgetFeedbackDispatcher uses to ensure its feedback channel.
    implementation(project(":core:notifications"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
