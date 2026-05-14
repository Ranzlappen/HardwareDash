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

    // WindowSizeClass is `api` because LocalWindowSizeClass exposes the
    // WindowSizeClass type in its public signature — every module that
    // reads `LocalWindowSizeClass.current` needs the dep on its classpath.
    api(libs.androidx.material3.window.size.class)
}
