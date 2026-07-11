// :feature:camera — CameraX barcode scanner (MLKit) with scan history.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.camera"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:datastore"))
    implementation(project(":core:root"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    // CameraActionHandler exposes the module's rooted extreme-tier
    // capabilities (CameraController) plus scan-history housekeeping.
    implementation(project(":core:automation"))

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.accompanist.permissions)
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

// CameraViewModelTest uses mockkStatic (to intercept ClipData.newPlainText) —
// mockk's inline mock maker needs to self-attach its instrumentation agent,
// which JDK 21 blocks by default unless this is set.
tasks.withType<Test> {
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
}
