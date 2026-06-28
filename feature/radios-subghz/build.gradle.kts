// :feature:radios-subghz — Sub-GHz SDR bridge: detects an attached SDR /
// Sub-GHz USB transceiver (RTL-SDR, HackRF, YardStick One, …) and surfaces
// its presence for monitoring and automation. The rooted flavor unlocks the
// raw-radio capability rows.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.subghz"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
