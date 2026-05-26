// :core:data — modular Room layer.
//
// Repo convention: Room lives here (see gadget.android.room). Other
// modules read through this module's repositories rather than depending
// on Room directly. First consumer: the :core:monitoring time-series
// store (MonitorSample), feeding the reusable monitoring container.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
    id("gadget.android.room")
}

android {
    namespace = "dev.ranzlappen.gadget.core.data"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
