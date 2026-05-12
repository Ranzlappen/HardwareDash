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

    // Design system is `api` because GadgetNavHost / GadgetAppShell
    // expose composables that take design-system types (GlassIntensity,
    // GadgetMotion specs) in their signatures or implementations.
    api(project(":core:designsystem"))
}
