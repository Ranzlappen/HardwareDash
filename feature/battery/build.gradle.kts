// :feature:battery — standard battery status and monitoring.
//
// Reads battery level, charging state, temperature, voltage, and health
// via BatteryManager broadcasts (no special permissions). Exposes three
// MetricSources (level / temperature / voltage) for the monitoring
// framework. The rooted battery extras (fuel-gauge, cell monitor,
// charging-profile override) will ship as :feature:battery-rooted.

plugins {
    id("gadget.android.feature")
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

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
