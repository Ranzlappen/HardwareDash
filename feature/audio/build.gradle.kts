// :feature:audio — microphone dB meter + WAV voice recording.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.audio"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    implementation(project(":core:root"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}

// mockk's inline agent (mockkStatic / mockkConstructor, used by
// AudioActionHandlerTest / AudioRecorderTest / DbMeterMetricSourceTest)
// self-attaches a ByteBuddy agent to the running JVM. JDK 9+ blocks a JVM
// from attaching to itself unless this is set explicitly.
tasks.withType<Test> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}
