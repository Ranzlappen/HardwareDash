// :feature:ambient — ambient light sensor readout and display-brightness info.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.ambient"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    // :core:widgetkit — the ambient-light home-screen widget rides the kit's
    // content/display archetype; :core:datastore backs its per-appWidgetId
    // config store via FeaturePreferencesFactory.
    implementation(project(":core:widgetkit"))
    implementation(project(":core:datastore"))
    // AmbientWidgetConfig is @Serializable (persisted per appWidgetId).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
