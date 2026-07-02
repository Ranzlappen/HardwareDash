// :feature:keepalive-rooted — rooted keep-alive controller.
//
// Sibling to :feature:keepalive, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:keepalive-rooted"))`. Provides
// RootedKeepAliveController (Doze whitelist via `cmd deviceidle` + `pm grant`
// of an allow-listed normal-permission set) + its Doze/PmGrant helpers, each
// routed through RootSafetyGate. Starts the shared PersistentKeepAliveService
// from the base module. Never reaches the standard APK (sourceSet scoping).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.keepalive.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // KeepAliveController contract (+ result / config types) + the shared
    // PersistentKeepAliveService live in the base :feature:keepalive module.
    implementation(project(":feature:keepalive"))
}
