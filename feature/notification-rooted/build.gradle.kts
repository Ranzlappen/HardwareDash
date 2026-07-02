// :feature:notification-rooted — rooted extreme-tier notification controller.
//
// Sibling to :feature:notification, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:notification-rooted"))`. Provides RootedNotificationController (sticky-notification override, listener-access grant, lock-screen overlay).
// Each privileged path is routed through RootSafetyGate; the standard no-op
// lives in the base :feature:notification module. Never reaches the standard APK
// (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.notification.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The notification controller contract (+ result / config types) lives in the
    // base :feature:notification module so both flavors share it.
    implementation(project(":feature:notification"))
    // The rooted helpers use coroutines primitives directly; :core:root
    // exposes coroutines only as `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
