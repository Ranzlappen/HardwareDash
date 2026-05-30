// :core:monitoring — the reusable monitoring framework.
//
// Provides the pluggable metric seam (consumed from :core:model), a
// per-metric persistent config, the sampling foreground service +
// determinate notification, the Vico chart, and the drop-in
// MonitorContainer composable any feature embeds to chart + persist a
// signal. NEVER depends on a feature module — features contribute their
// MetricSource / MonitorWidgetNotifier via Hilt map multibindings that
// resolve at :app assembly.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.core.monitoring"
}

dependencies {
    // `api` so embedding features reach DashCard/GadgetSlider/etc., the
    // MetricSource contract, and the MonitorBucket/MonitorSample data types
    // that MonitorChart/MonitorHistory expose, without an extra dependency
    // line.
    api(project(":core:ui"))
    api(project(":core:model"))
    api(project(":core:data"))
    implementation(project(":core:datastore"))
    // :core:notifications — MonitorService registers its
    // determinate-progress channel through NotificationChannelRegistry.
    implementation(project(":core:notifications"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
