// :core:designsystem — Gadget Material 3 design system (theme + tokens + glassmorphism).

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
}

android {
    namespace = "dev.ranzlappen.gadget.core.designsystem"
}

dependencies {
    // Material 3 + icons + base Compose UI surfaced as `api` so any
    // module that depends on `:core:designsystem` picks up the full
    // toolkit without redeclaring. core/ui depends on this module and
    // re-exports it via its own `api` dep; feature modules go through
    // the `gadget.android.feature` convention plugin which already
    // brings material3 separately, but pulling them in here keeps the
    // design system self-contained for direct dependents.
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
}
