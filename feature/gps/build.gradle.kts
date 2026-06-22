// :feature:gps — GPS / Location feature module.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.gps"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:root"))

    // FusedLocationProvider — standard SDK (no root).
    implementation(libs.play.services.location)
    // Runtime permission helper.
    implementation(libs.accompanist.permissions)
    // Map tiles — OSMDroid, no API key needed.
    implementation(libs.osmdroid.android)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
