// :core:widgetkit — reusable home-screen widget framework.
//
// The generic half of the torch widget subsystem, extracted so a 30-module
// app doesn't re-hand-roll the pin flow, per-appWidgetId persistence,
// RemoteViews appearance rendering, the icon catalog, and toast/notification
// feedback for every feature. Features plug in via a config implementing
// WidgetKitConfig; the kit never depends on a feature module.
//
// Status (Phase 2 / refactor-2026): this batch establishes the module + the
// generic seam (config contract, shared receiver scope, count/cap policy +
// pin-result type). The resource- and Hilt-coupled layer (WidgetAppearance
// value types, the RemoteViews renderer, the icon catalog, the providers, and
// the pin-flow store) moves here in a follow-up done WITH a compiler in the
// loop — that migration touches Android resource merging, the Hilt graph, and
// kotlinx.serialization wire format (the polymorphic ToggleFeedback
// discriminator), none of which CI-less editing can verify safely. See
// docs/refactor-2026/progress.md.

plugins {
    id("gadget.android.library")
}

android {
    namespace = "dev.ranzlappen.gadget.core.widgetkit"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
