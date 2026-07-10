// :feature:display — display controller contract + standard no-op.
//
// Screenless rooted-extras feature: the DisplayController contract (brightness
// / density / refresh-rate overrides + SurfaceFlinger dump) and the standard
// no-op live here so shared code binds one interface in both flavors; the
// privileged impl ships in :feature:display-rooted. Surfaced in-app via the
// RootedDisplayExtrasSections composable in :app.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.display"
}

dependencies {
    // RootCapabilityRegistry — the flavor seam DisplayViewModel reads
    // instead of branching on BuildConfig.IS_ROOTED.
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
