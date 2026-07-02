// :feature:diagnostics — system diagnostics capability overview.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.diagnostics"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
}
