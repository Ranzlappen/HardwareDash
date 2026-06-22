// :feature:manual — user-facing help and documentation screen.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.manual"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
}
