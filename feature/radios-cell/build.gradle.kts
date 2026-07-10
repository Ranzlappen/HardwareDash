// :feature:radios-cell — Cellular diagnostics controller contract + the
// standard-tier TelephonyManager screen (SIM / network type / signal).
//
// The CellController interface, its result type, and the standard-flavor
// no-op are shared with the rooted sibling :feature:radios-cell-rooted
// module (and both flavors' RootBindings). Everything else here — the
// screen, ViewModel, MetricSource, and ActionHandler — is standard-tier,
// built directly on android.telephony (no new library dependency needed).

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.radios.cell"
}

dependencies {
    // Shared with the rooted sibling and both flavors' RootBindings.
    implementation(project(":core:root"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monitoring"))
    implementation(project(":core:automation"))
    // Runtime permission helper (READ_PHONE_STATE request flow) — same
    // dependency :feature:gps uses for ACCESS_FINE_LOCATION.
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
