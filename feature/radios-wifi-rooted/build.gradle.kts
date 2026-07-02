// :feature:radios-wifi-rooted — rooted sibling of :feature:radios-wifi.
//
// Exposes the privileged Wi-Fi radio controls (rfkill block/unblock, iw
// TX-power override capped at the 20 dBm regulatory ceiling, iw channel
// override restricted to a regulatory allow-list, and a read-only
// monitor/IBSS injection-capability probe) to the automation engine as the
// `wifi_root` ActionHandler, each call safety-gated by RootSafetyGate. Pulled
// into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:radios-wifi-rooted"))`, so the
// standard APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.wifi.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))
    // The Wi-Fi controller contract (WifiController + config/result types) lives
    // in the base :feature:radios-wifi module so both flavors share it.
    implementation(project(":feature:radios-wifi"))

    testImplementation(libs.junit)
}
