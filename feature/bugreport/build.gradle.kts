// :feature:bugreport — app permission health overview screen.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.bugreport"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:automation"))
    // :core:model — the MetricSource contract this module's
    // BugreportMetricSource implements.
    implementation(project(":core:model"))
    // :core:monitoring — the reusable monitoring container + MetricSource
    // sampler seam (api-exposes :core:ui and :core:model).
    implementation(project(":core:monitoring"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
