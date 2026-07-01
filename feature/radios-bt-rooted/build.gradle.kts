// :feature:radios-bt-rooted — rooted sibling of :feature:radios-bt.
//
// Hosts the privileged Bluetooth controller (rfkill block/unblock, TX-power
// override capped at the 10 dBm Class-1 ceiling via bluetoothctl/hcitool, and
// a read-only HCI-snoop-log tail), each call safety-gated by RootSafetyGate.
// Pulled into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:radios-bt-rooted"))`, so the
// standard APK never sees it.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.bt.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The Bluetooth controller contract (BluetoothController + config/result
    // types) lives in the base :feature:radios-bt module so both flavors share it.
    implementation(project(":feature:radios-bt"))
    // RootedBluetoothController uses coroutines primitives (NonCancellable,
    // delay, withContext) directly; :core:root exposes them only as
    // `implementation`, so declare them here.
    implementation(libs.kotlinx.coroutines.core)
}
