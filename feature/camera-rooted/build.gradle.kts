// :feature:camera-rooted — rooted extreme-tier camera controller.
//
// Sibling to :feature:camera, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:camera-rooted"))`. Provides the
// RootedCameraController (high-FPS / manual-exposure / RAW-DNG / multi-camera
// / HAL-bypass / shutter-sound) + its Camera2 + sysfs helpers, each routed
// through RootSafetyGate. The standard-flavor no-op lives in the base
// :feature:camera module so shared code binds one interface in both flavors.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.camera.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The CameraController contract (+ result / config types) lives in the
    // base :feature:camera module so both flavors share it.
    implementation(project(":feature:camera"))
    // The Camera2 helpers use coroutines primitives directly (NonCancellable,
    // async, withContext, suspendCancellableCoroutine); :core:root exposes
    // coroutines only as `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
