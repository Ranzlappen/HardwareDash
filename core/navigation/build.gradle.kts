// :core:navigation — typed destinations + NavHost + AppShell for Gadget.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
}

android {
    namespace = "dev.ranzlappen.gadget.core.navigation"
}

dependencies {
    // Navigation Compose surfaced as `api` so feature modules that
    // depend on this module can register `NavGraphBuilder.<screen>()`
    // extensions without redeclaring the dep.
    api(libs.androidx.navigation.compose)

    // Design system is `api` because GadgetNavHost / GadgetApp
    // expose composables that take design-system types (GlassIntensity,
    // GadgetMotion specs) in their signatures or implementations.
    api(project(":core:designsystem"))

    // :core:ui surfaces ModuleScreenScaffold, used internally by
    // ComingSoonScreen for the four placeholder destinations. Kept as
    // `implementation` (not `api`) because the scaffold type does not
    // leak into any public signature in :core:navigation — downstream
    // modules don't need it transitively.
    implementation(project(":core:ui"))
}
