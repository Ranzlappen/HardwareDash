// :core:root — root safety + capability framework.
//
// The pure-Kotlin contract layer that gates every privileged (rooted)
// mutation in the app:
//   * RootSafetyGate — per-feature opt-out + rate-limit + capability
//     gate. Every rooted controller routes its privileged calls through
//     `gate.guarded(featureKey) { … }` so the user always has a kill
//     switch and runaway invocations can't escape the limiter.
//   * RootCapabilityRegistry — runtime capability probe (root provider
//     present, mount-rw available, ABI quirks, …) the gate consults.
//   * RootSoftLimiter — token-bucket rate-limit on per-feature
//     invocations to bound battery / thermal / wear cost.
//   * RootFeatureToggles + RootFeatureRegistry — user-facing opt-in
//     surface; backed by a DataStore for per-feature persistence.
//   * RootShell + RootService + RootDetector — the libsu-free
//     interfaces every rooted impl satisfies. Concrete implementations
//     stay flavor-scoped (libsu lives in `app/src/rooted/`).
//   * Companion / launch / emergency / sysfs — supporting infra:
//     companion-module detection, fatal-launch gate, emergency reset
//     coordinator, sysfs mutation log.
//
// Pure Kotlin + Hilt + DataStore. Zero Compose, zero feature-controller
// deps — those would create cycles (`:app` depends on this module, not
// the other way). The 13 root-UI Compose composables under
// `com.gadget.root.ui.*` stay in `:app` until each feature controller
// it targets has moved to its own modular feature module.
//
// Extracted in refactor-2026 Phase 2 / Batch D1. Standard / rooted
// flavor impls of these interfaces stay in `app/src/{standard,rooted}/`
// for D2 + D3 to repackage and bind.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.root"
}

dependencies {
    // DataStore drives the RootFeatureToggles user-opt-in store + the
    // RootSafetyPreferences event log persistence. `api` because the
    // toggles' Flow shape surfaces through to consumers.
    api(project(":core:datastore"))

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    // kotlin-test — RootSafetyPreferences / AlsaMixerControl /
    // RootFeatureRegistry tests use kotlin.test assertions. Declared
    // explicitly because this is a `gadget.android.library` module (the
    // feature convention plugin would otherwise add it); without it the test
    // source does not compile, which is why the module was historically
    // absent from the CI test list.
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
