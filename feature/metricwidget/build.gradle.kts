// :feature:metricwidget — the generic configurable metric home-screen widget (W4).
//
// The first cross-cutting widget: it binds to any registered MetricSource the
// user picks in its configure activity (rather than a single feature's
// hardwired signal), so it depends on :core:monitoring for the app-wide
// Map<String, MetricSource> multibinding and rides the :core:widgetkit
// content/display archetype (BaseContentWidgetProvider) with a
// :core:datastore-backed per-appWidgetId config store.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.metricwidget"
}

dependencies {
    implementation(project(":core:ui"))
    // MetricSource seam + the Map<String, MetricSource> @Multibinds (also
    // transitively exposes :core:model which defines MetricDescriptor);
    // MonitorChartBitmapRenderer + MonitorDownsampling back the sparkline.
    implementation(project(":core:monitoring"))
    // :core:data — MonitorSampleRepository supplies the windowed history the
    // sparkline display mode renders.
    implementation(project(":core:data"))
    // :core:widgetkit — the content/display archetype (BaseContentWidgetProvider,
    // ContentWidgetCustomizationSheet); :core:datastore backs the per-appWidgetId
    // config store via FeaturePreferencesFactory.
    implementation(project(":core:widgetkit"))
    implementation(project(":core:datastore"))
    // MetricWidgetConfig is @Serializable (persisted per appWidgetId).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
