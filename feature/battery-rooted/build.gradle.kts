// :feature:battery-rooted — rooted extreme-tier battery controller.
//
// Sibling to :feature:battery, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:battery-rooted"))`. Provides the
// RootedBatteryController (fuel-gauge / cell monitor / charging-profile +
// charging-type override / hold-SoC loop / thermal-throttle bypass / wireless
// coil-current) + its power-supply sysfs, thermal-zone, health-reader, and
// dump-writer helpers, each routed through RootSafetyGate. The standard-flavor
// no-op lives in the base :feature:battery module so shared code binds one
// interface in both flavors.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.battery.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The BatteryController contract (+ result / CellReading / config types)
    // lives in the base :feature:battery module so both flavors share it.
    implementation(project(":feature:battery"))
    // The rooted helpers use coroutines primitives directly (NonCancellable,
    // delay, withContext); :core:root exposes coroutines only as
    // `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
