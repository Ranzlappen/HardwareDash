// :feature:motion — gyroscope, step counter, and motion-detect sensors.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.motion"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:hardware"))  // DeviceSensors
    implementation(project(":core:root"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
