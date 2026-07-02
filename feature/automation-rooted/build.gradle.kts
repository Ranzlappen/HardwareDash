// :feature:automation-rooted — rooted extreme-tier automation controller.
//
// Sibling to :feature:automation, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:automation-rooted"))`. Provides RootedAutomationController (privileged intent broadcast/start, system-settings override, dumpsys).
// Each privileged path is routed through RootSafetyGate; the standard no-op
// lives in the base :feature:automation module. Never reaches the standard APK
// (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.automation.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The automation controller contract (+ result / config types) lives in the
    // base :feature:automation module so both flavors share it.
    implementation(project(":feature:automation"))
}
