// :feature:bugreport — app permission health overview screen.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.bugreport"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:automation"))
}
