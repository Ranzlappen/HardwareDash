// :feature:radios-nfc — NFC tag read/write + HCE emulation.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.nfc"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    implementation(project(":core:root"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}

// mockk's inline agent (mockkStatic / mockkConstructor, used by
// NfcActionHandlerTest) self-attaches a ByteBuddy agent to the running JVM.
// JDK 9+ blocks a JVM from attaching to itself unless this is set explicitly.
tasks.withType<Test> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}
