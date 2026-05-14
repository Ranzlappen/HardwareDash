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
    //
    // Alias is `androidx-material3-windowsizeclass` (one word) — Gradle 8.7
    // reserves `class` as a hyphen-separated alias segment, so the
    // natural `material3-window-size-class` artefact-style alias is
    // rejected. The artefact name itself is unchanged.
    api(libs.androidx.material3.windowsizeclass)

    // Test fixtures consumed by androidTest source set in this module.
    // :core:testing re-exports ui-test-junit4 (+ ui-test-manifest as
    // debugImplementation) so a single line plugs in the full Compose
    // UI test stack.
    androidTestImplementation(project(":core:testing"))
}
