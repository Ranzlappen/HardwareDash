// :feature:lock — device lock state monitor and biometric availability.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.lock"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

// mockk's inline agent (mockkStatic, used to intercept BiometricManager.from(...)
// and ContextCompat.registerReceiver(...)) self-attaches a ByteBuddy agent to
// the running JVM. JDK 9+ blocks a JVM from attaching to itself unless this
// is set explicitly.
tasks.withType<Test> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}
