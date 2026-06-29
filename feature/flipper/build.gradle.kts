// :feature:flipper — Flipper Zero bridge. USB CDC-ACM + BLE GATT transport,
// a hand-rolled protobuf RPC stack, and command suites (System / Storage /
// Sub-GHz / Infrared). Monitoring + automation ready; the rooted sibling
// :feature:flipper-rooted adds root-granted USB access.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.flipper"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    implementation(libs.usb.serial.android)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
