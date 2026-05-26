// :core:automation — the per-module action contract for the future
// automation tool.
//
// Holds only the discovery/dispatch CONTRACT (ModuleAction metadata +
// ActionHandler + ModuleActionRegistry): a feature contributes its
// invocable actions into a Hilt map so the automation engine can drive
// any module without the central hardcoding the legacy Link module had.
// The rule model / evaluator / builder UI live in the final automation
// feature (:feature:automation-ui), NOT here.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.automation"
}
