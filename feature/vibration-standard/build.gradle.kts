// :feature:vibration-standard — standard-flavor no-op Vibration capability
// surface. Mirror of :feature:vibration-rooted.
//
// Holds the no-op twin of the privileged VibrationRootCapabilities so the
// standard APK injects the same modular interface the rooted flavor does —
// without ever compiling against root code. Pulled in exclusively by the
// standard flavor of :app via `standardImplementation`.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.vibration.standard"
}

dependencies {
    // Modular Vibration interfaces these no-ops implement
    // (VibrationRootCapabilities + its result/availability types). No
    // :core:root, no libsu — the standard surface is inert by construction.
    implementation(project(":feature:vibration"))
}
