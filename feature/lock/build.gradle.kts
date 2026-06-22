// :feature:lock — device lock state monitor and biometric availability.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.lock"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    implementation(libs.androidx.biometric)
}
