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

    // Automation engine core (ADR-0002): the RuleRepository contract + the
    // Rule model whose sealed graphs persist as JSON columns in
    // automation.db. The serialization runtime is needed to invoke the
    // model's serializers from RuleMapper (no @Serializable declarations
    // here, so the serialization *plugin* is not applied).
    implementation(project(":core:automation"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    // F3 engine integration test (rule -> Room -> gate -> evaluator ->
    // dispatch) runs on-device against an in-memory AutomationDatabase.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
