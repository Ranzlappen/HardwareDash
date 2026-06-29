// :feature:flipper-rooted — rooted sibling of :feature:flipper. Relaxes the
// attached Flipper's USB device-node permissions via root so the CDC-ACM port
// opens without the per-attach permission dialog. Pulled into the rooted
// flavor of :app via `rootedImplementation(project(":feature:flipper-rooted"))`.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.flipper.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))
    // Reuses FlipperUsbLink.listDevices() + the Flipper VID/PID.
    implementation(project(":feature:flipper"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
