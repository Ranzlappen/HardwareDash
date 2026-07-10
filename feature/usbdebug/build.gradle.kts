// :feature:usbdebug — USB-debugging screen, controller contract + standard no-op
// (function switch, device dump, serial-service dump).
//
// The controller contract + standard no-op live here so shared code binds one
// interface in both flavors; the privileged impl ships in
// :feature:usbdebug-rooted. This module also owns the full Module Authoring
// Contract screen (UsbDebugScreen / UsbDebugScreenContent), a standard-tier
// `usb_debugging` MetricSource reading Settings.Global.ADB_ENABLED, and a
// rooted UsbDebugActionHandler wrapping the four controller methods.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.usbdebug"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
