// :feature:motion — gyroscope, step counter, and motion-detect sensors.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.motion"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:hardware"))  // DeviceSensors
    implementation(project(":core:root"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    // Motion exposes threshold-assert actions over its existing MetricSources
    // so the automation engine can gate a rule on a sensor reading.
    implementation(project(":core:automation"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
