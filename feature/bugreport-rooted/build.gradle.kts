// :feature:bugreport-rooted — rooted sibling of :feature:bugreport. Force-grants
// a declared runtime permission to this app via `pm grant`, bypassing the system
// permission dialog. Pulled into the rooted flavor of :app via
// `rootedImplementation(project(":feature:bugreport-rooted"))`.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.bugreport.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
