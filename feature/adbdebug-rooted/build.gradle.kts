// :feature:adbdebug-rooted — rooted extreme-tier adbdebug controller.
//
// Sibling to :feature:adbdebug, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:adbdebug-rooted"))`. Provides RootedAdbDebuggingController (wireless-ADB rfkill/settings toggle, setprop, prop dump).
// Each privileged path is routed through RootSafetyGate; the standard no-op
// lives in the base :feature:adbdebug module. Never reaches the standard APK
// (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.adbdebug.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The adbdebug controller contract (+ result / config types) lives in the
    // base :feature:adbdebug module so both flavors share it.
    implementation(project(":feature:adbdebug"))
}
