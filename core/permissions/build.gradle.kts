// :core:permissions (W5) — the centralized permission framework: a
// per-feature permission registry (Hilt @IntoMap), grant-state queries,
// special-permission (overlay / exact-alarm / WRITE_SETTINGS / notification-
// listener / all-files) settings-intent orchestration, and a reusable
// Permissions dashboard card. Compose + Hilt core module (the
// :core:monitoring plugin stanza); activity-compose is added explicitly
// since only the feature plugin pulls it in transitively.

plugins {
    id("gadget.android.library")
    id("gadget.android.library.compose")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.permissions"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
}
