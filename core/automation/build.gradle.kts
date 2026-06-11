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
//   * The RUNTIME HOST (engine-milestone Batch F): AutomationService (the
//     specialUse FGS that subscribes metric-stream triggers and dispatches)
//     + AutomationController. Reads signals via :core:model's MetricSource,
//     gates root via :core:root, and posts its FGS notification via
//     :core:notifications. Still imports NO :feature module.

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
    // RuleRepository exposes Flow. `api` so consumers of the contract see
    // the type without re-declaring the dep (mirrors :core:model).
    api(libs.kotlinx.coroutines.core)

    // Runtime host (Batch F): the read seam (MetricSource), root gating, and
    // the FGS notification channel. NO :feature module, NO :core:data (the
    // RuleRepository contract lives here; :core:data implements it).
    implementation(project(":core:model"))
    implementation(project(":core:root"))
    implementation(project(":core:notifications"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
