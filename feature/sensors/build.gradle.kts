// :feature:sensors — sensor explorer (v3 B5): every sensor on the device,
// live detail per sensor, and the sensor MetricSources (incl. the ones that
// used to live in :feature:motion / :feature:ambient — keys unchanged).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.sensors"
}

dependencies {
    // :core:ui brings the design system; :core:navigation the routes.
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // :core:monitoring for the embedded MonitorContainer history charts; its
    // `api` surface also brings :core:model (MetricSource — the signal
    // contract the sensor sources implement).
    implementation(project(":core:monitoring"))
    // DeviceSensors — the SensorManager seam (list, streams, one-shots).
    implementation(project(":core:hardware"))

    // Stateless-screen instrumented test (Compose UI test stack via the
    // shared fixtures, the torch/vibration pattern).
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
