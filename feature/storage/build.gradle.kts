// :feature:storage — Storage volumes monitor.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.storage"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:root"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
