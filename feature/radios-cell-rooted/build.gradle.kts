// :feature:radios-cell-rooted — rooted sibling of :feature:radios-cell.
//
// Hosts the privileged Cellular controller (read-only modem/signal
// deep-dump over Qualcomm-style sysfs nodes via the root shell), gated by
// RootSafetyGate. Pulled into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:radios-cell-rooted"))`, so the
// standard APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.cell.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The Cellular controller contract (CellController + result type) lives
    // in the base :feature:radios-cell module so both flavors share it.
    implementation(project(":feature:radios-cell"))
}
