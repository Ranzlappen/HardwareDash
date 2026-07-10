// :feature:microphone — rooted-only "extreme mic tools" screen + controller
// contract.
//
// The MicrophoneController contract (mic-gain boost / direct-PCM /
// custom-sample-rate / multi-mic capture / audio-effect override / system-
// audio capture) and the standard no-op live here so shared code binds one
// interface in both flavors; the privileged impl ships in
// :feature:microphone-rooted. This module also owns the screen: every row is
// rooted-only, so on the standard flavor the whole panel renders disabled
// with an explanatory badge. Baseline mic capture (dB meter / voice
// recording) stays in :feature:audio — never imported from here.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.microphone"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
