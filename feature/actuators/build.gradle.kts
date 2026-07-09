// :feature:actuators — vibration actuator capabilities and rooted PWM control.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.actuators"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:automation"))
    // :core:monitoring — the reusable monitoring container + MetricSource
    // seam (api-exposes :core:ui and :core:model). Actuators contributes an
    // ActuatorsMetricSource for the modelled haptic-pulse signal.
    implementation(project(":core:monitoring"))
}
