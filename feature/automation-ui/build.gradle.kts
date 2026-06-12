// :feature:automation-ui — the automation rules list + rule builder
// (docs/automation-engine.md batch 3.4; epic #145). Consumes the two
// enumeration seams — HardwareRegistry (read side) and
// ModuleActionRegistry (write side) — and never imports a feature module.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.automation.ui"
}

dependencies {
    // :core:ui brings the design system; :core:navigation the routes.
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // The engine: rule model + RuleRepository contract + the runtime surfaces
    // the builder wires on save (AutomationScheduler / AutomationController /
    // RuleFireExecutor for the manual "run now" path).
    implementation(project(":core:automation"))
    // Read-side enumeration for the trigger/condition signal pickers.
    implementation(project(":core:hardware"))
    // MetricDescriptor (also surfaced via :core:hardware's api, declared
    // explicitly because the pickers consume it directly).
    implementation(project(":core:model"))
    // RootCapabilityRegistry — the builder-side root-action filter
    // (gating layer 1).
    implementation(project(":core:root"))
    implementation(libs.kotlinx.coroutines.android)

    // Stateless-screen instrumented test (Compose UI test stack via the
    // shared fixtures, the torch/vibration/sensors pattern).
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
