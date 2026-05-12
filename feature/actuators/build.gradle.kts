// =========================================================================
// :feature:actuators — Batch 0 skeleton.
//
// Empty Android library. Inline plugin application will be replaced by
// the `gadget.android.feature` convention plugin in Batch 1.
// =========================================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.actuators"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
