// :feature:radios-wifi — WiFi status and network information.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.wifi"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
