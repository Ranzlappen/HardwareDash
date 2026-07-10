// :feature:adbdebug — ADB debugging feature module (network toggle, prop dump, setprop).
//
// Owns the full module surface: AdbDebugScreen (standard debug-state readout +
// Settings deep-link; rooted toggle / network / getprop dump / setprop editor),
// AdbEnabledMetricSource, and AdbDebugActionHandler. The controller contract +
// standard no-op live here so shared code binds one interface in both flavors;
// the privileged impl ships in :feature:adbdebug-rooted (a sibling module this
// one never depends on — see that module's build.gradle.kts).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.adbdebug"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    implementation(project(":core:root"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
