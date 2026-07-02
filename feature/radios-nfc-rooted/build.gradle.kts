// :feature:radios-nfc-rooted — rooted sibling of :feature:radios-nfc.
//
// Hosts the privileged NFC controller (raw NCI command exchange over the
// vendor sysfs node, with a 256-byte payload ceiling + 5 s read-timeout),
// gated by RootSafetyGate. Pulled into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:radios-nfc-rooted"))`, so the
// standard APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.nfc.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The NFC controller contract (NfcController + config/result types) lives
    // in the base :feature:radios-nfc module so both flavors share it.
    implementation(project(":feature:radios-nfc"))
}
