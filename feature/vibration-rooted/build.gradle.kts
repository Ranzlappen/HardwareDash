// :feature:vibration-rooted — rooted-only Vibration capability surface.
//
// Sibling to :feature:vibration, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:vibration-rooted"))`. The standard
// APK is physically unable to compile against this module — see CLAUDE.md's
// "Standard-APK leak gate".
//
// Contains the libsu-backed RootedVibrationController (direct sysfs PWM), the
// VibrationSysfsPaths probe, the DualActuatorDriver + RumbleMonitor helpers,
// and the RootedVibrationRootCapabilities adapter that exposes the privileged
// surface through :feature:vibration's modular VibrationRootCapabilities
// interface. Every privileged write routes through :core:root's
// RootSafetyGate. Mirror of :feature:torch-rooted.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.vibration.rooted"
}

dependencies {
    // Modular Vibration interfaces this module implements.
    implementation(project(":feature:vibration"))
    // Root-safety framework (RootSafetyGate, RootCapabilityRegistry,
    // RootFeatureKey, RootShell, RootGateDecision).
    implementation(project(":core:root"))

    // libsu — the rooted shell binder backing RootShell. Scoped to this
    // module's rooted-flavor binding; the standard APK never reaches this dep.
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
}
