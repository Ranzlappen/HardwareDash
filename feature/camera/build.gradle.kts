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
}
