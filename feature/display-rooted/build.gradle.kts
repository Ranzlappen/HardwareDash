// :feature:display-rooted — rooted extreme-tier display controller.
//
// Sibling to :feature:display, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:display-rooted"))`. Provides the
// RootedDisplayController (backlight sysfs brightness, density + refresh-rate
// override via `wm`/`cmd`, SurfaceFlinger dump) + its helpers, each routed
// through RootSafetyGate. The standard no-op lives in the base :feature:display
// module so shared code binds one interface in both flavors. Never reaches the
// standard APK (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.display.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The DisplayController contract (+ result / config types) lives in the
    // base :feature:display module so both flavors share it.
    implementation(project(":feature:display"))
    // The rooted helpers use coroutines primitives directly; :core:root exposes
    // coroutines only as `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
