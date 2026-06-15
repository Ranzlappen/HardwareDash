// :feature:radios-bt — Bluetooth status and device enumeration.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.bt"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
