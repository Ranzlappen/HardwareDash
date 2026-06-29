// :feature:storage — Storage volumes monitor.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.storage"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:root"))
    // :core:widgetkit — the storage home-screen widget rides the kit's
    // content/display archetype; :core:datastore backs its per-appWidgetId
    // config store via FeaturePreferencesFactory.
    implementation(project(":core:widgetkit"))
    implementation(project(":core:datastore"))
    // StorageWidgetConfig is @Serializable (persisted per appWidgetId).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
