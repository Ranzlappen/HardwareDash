// :core:automation — the automation contract + engine core.
//
// Two layers (see docs/automation-engine.md + ADR-0002):
//   * The discovery/dispatch CONTRACT (ModuleAction metadata +
//     ActionHandler + ModuleActionRegistry): a feature contributes its
//     invocable actions into a Hilt map so the engine can drive any module
//     without the central hardcoding the legacy Link module had.
//   * The ENGINE CORE (engine-milestone Batch E): the persisted rule model
//     (Rule / Trigger / Condition / RuleAction, every sealed subtype with a
//     pinned @SerialName), the pure-Kotlin RuleEvaluator, and the
//     RuleRepository contract. The Room implementation lives in :core:data;
//     the Compose builder UI lives in :feature:automation-ui — NOT here.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.core.automation"
}

dependencies {
    // Rule / Trigger / Condition persist as kotlinx-serialization JSON
    // (the automation.db rules table stores the sealed graphs as JSON
    // columns — ADR-0002 Decision 5).
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
