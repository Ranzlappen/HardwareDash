// :feature:microphone-rooted — rooted extreme-tier microphone controller.
//
// Sibling to :feature:microphone, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:microphone-rooted"))`. Provides the
// RootedMicrophoneController (mic-gain boost via ALSA mixer, direct-PCM, custom
// sample rate, multi-mic + system-audio capture, audio-effect override), each
// routed through RootSafetyGate. The shared `AlsaMixerControl` tinymix wrapper
// lives in :core:root (also used by :feature:audio-rooted). Never reaches the
// standard APK (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.microphone.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The MicrophoneController contract (+ result / config types) lives in the
    // base :feature:microphone module so both flavors share it.
    implementation(project(":feature:microphone"))
    // The rooted helpers use coroutines primitives directly; :core:root exposes
    // coroutines only as `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
