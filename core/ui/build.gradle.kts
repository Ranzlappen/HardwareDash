// :core:ui — higher-level reusable composables built on :core:designsystem.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
}

android {
    namespace = "dev.ranzlappen.gadget.core.ui"
}

dependencies {
    // `api` so feature modules can `implementation(project(":core:ui"))`
    // and reach designsystem types (GadgetTheme, GlassIntensity, …)
    // without an extra dependency line.
    api(project(":core:designsystem"))
}
