// :feature:microphone — microphone controller contract + standard no-op.
//
// Screenless rooted-extras feature: the MicrophoneController contract
// (mic-gain boost / direct-PCM / custom-sample-rate / multi-mic capture +
// audio-effect override) and the standard no-op live here so shared code binds
// one interface in both flavors; the privileged impl ships in
// :feature:microphone-rooted. Surfaced in-app via RootedAvExtrasSections.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.microphone"
}
