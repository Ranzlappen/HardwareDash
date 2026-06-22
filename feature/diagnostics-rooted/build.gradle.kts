// :feature:diagnostics-rooted — rooted diagnostics actions (logcat, meminfo, cpuinfo, procstats).
//
// Sibling to :feature:diagnostics, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:diagnostics-rooted"))`. Provides
// RootedDiagnosticsActionHandler bound under featureId "diagnostics_root", which
// exposes the four safety-gated shell dump actions to the automation engine.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.diagnostics.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))
}
