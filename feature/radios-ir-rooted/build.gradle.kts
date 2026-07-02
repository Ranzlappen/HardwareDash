// :feature:radios-ir-rooted — rooted sibling of :feature:radios-ir.
//
// Hosts the privileged IR controller (custom carrier frequency via LIRC and
// direct IR-LED GPIO toggling via sysfs, each with hard duty-cycle / burst /
// frequency ceilings), gated by RootSafetyGate. Pulled into the rooted flavor
// of :app only via `rootedImplementation(project(":feature:radios-ir-rooted"))`,
// so the standard APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.ir.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The IR controller contract (IrController + config/result types) lives in
    // the base :feature:radios-ir module so both flavors share it.
    implementation(project(":feature:radios-ir"))
    // RootedIrController + IrLedSysfsHelper use coroutines primitives
    // (NonCancellable, delay, withContext) directly; :core:root exposes them
    // only as `implementation`, so declare them here.
    implementation(libs.kotlinx.coroutines.core)
}
