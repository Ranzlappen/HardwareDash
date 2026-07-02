// :feature:usbdebug-rooted — rooted extreme-tier usbdebug controller.
//
// Sibling to :feature:usbdebug, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:usbdebug-rooted"))`. Provides RootedUsbDebuggingController (USB function switch, device-node dump, serial-service dump).
// Each privileged path is routed through RootSafetyGate; the standard no-op
// lives in the base :feature:usbdebug module. Never reaches the standard APK
// (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.usbdebug.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The usbdebug controller contract (+ result / config types) lives in the
    // base :feature:usbdebug module so both flavors share it.
    implementation(project(":feature:usbdebug"))
}
