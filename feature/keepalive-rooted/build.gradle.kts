// :feature:keepalive-rooted — rooted keep-alive controller.
//
// Sibling to :feature:keepalive, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:keepalive-rooted"))`. Provides the
// RootedKeepAliveController (Doze whitelist via `cmd deviceidle` + `pm grant`
// of an allow-listed permission set) + its DozeBypassHelper / PmGrantHelper,
// each routed through RootSafetyGate and logged for revert. The standard no-op
// lives in the base :feature:keepalive module so shared code binds one
// interface in both flavors. Never reaches the standard APK (sourceSet scoping
// keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.keepalive.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The KeepAliveController contract (+ result / config types) and the shared
    // PersistentKeepAliveService live in the base :feature:keepalive module.
    implementation(project(":feature:keepalive"))
    // ContextCompat.startForegroundService for the rooted controller.
    implementation(libs.androidx.core.ktx)
    // The rooted controller/helpers use coroutines primitives directly.
    implementation(libs.kotlinx.coroutines.core)
}
