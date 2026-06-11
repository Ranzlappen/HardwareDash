// :feature:sensors — skeleton; configuration via gadget.android.feature.

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
    // contract the three sensor sources implement).
    implementation(project(":core:monitoring"))

    // Stateless-screen instrumented test (Compose UI test stack via the
    // shared fixtures, the torch/vibration pattern).
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
