// :feature:audio-rooted — rooted extreme-tier audio-routing controller.
//
// Sibling to :feature:audio, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:audio-rooted"))`. Provides the
// RootedAudioRoutingController (stream-volume bypass, force-routing via `cmd`,
// mute-all via the shared ALSA mixer, dumpsys), each routed through
// RootSafetyGate. The shared `AlsaMixerControl` tinymix wrapper lives in
// :core:root (also used by :feature:microphone-rooted). Never reaches the
// standard APK (sourceSet scoping keeps the privileged code out of the leak gate).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.audio.rooted"
}

dependencies {
    implementation(project(":core:root"))
    // The AudioRoutingController contract (+ result / config types) lives in the
    // base :feature:audio module so both flavors share it.
    implementation(project(":feature:audio"))
    // The rooted helpers use coroutines primitives directly; :core:root exposes
    // coroutines only as `implementation`, so declare it here.
    implementation(libs.kotlinx.coroutines.core)
}
