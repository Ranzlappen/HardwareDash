// :core:hardware — the read-side enumeration layer (epic #146).
//
// The symmetric partner to :core:automation's ModuleActionRegistry: actions
// on the write side, signals on the read side. HardwareRegistry aggregates
// the Hilt `Map<String, MetricSource>` multibinding (the SAME map the
// monitoring sampler and the automation engine consume — one signal
// definition, never two) so the automation rule-builder and any future
// hardware browser can enumerate "what signals exist on this device"
// without importing any feature module.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.hardware"
}

dependencies {
    // MetricSource / MetricDescriptor — the one readable-signal contract.
    // `api`: the registry's surface IS descriptor-shaped.
    api(project(":core:model"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
