// :feature:battery — standard battery status and monitoring.
//
// Reads battery level, charging state, temperature, voltage, and health
// via BatteryManager broadcasts (no special permissions). Exposes three
// MetricSources (level / temperature / voltage) for the monitoring
// framework. The rooted battery extras (fuel-gauge, cell monitor,
// charging-profile override) will ship as :feature:battery-rooted.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.battery"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // MonitorContainer + MetricSource seam (also transitively exposes
    // :core:model which defines MetricDescriptor / MetricCategory).
    implementation(project(":core:monitoring"))
    implementation(project(":core:root"))
    // :core:widgetkit — the battery home-screen widget rides the kit's
    // content/display archetype (BaseContentWidgetProvider); :core:datastore
    // backs the per-appWidgetId config store via FeaturePreferencesFactory.
    implementation(project(":core:widgetkit"))
    implementation(project(":core:datastore"))
    // BatteryWidgetConfig is @Serializable (persisted per appWidgetId).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
