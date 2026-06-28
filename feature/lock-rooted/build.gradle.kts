// :feature:lock-rooted — rooted sibling of :feature:lock. Draws a bounded
// `TYPE_APPLICATION_OVERLAY` above the secure keyguard (granting
// SYSTEM_ALERT_WINDOW via root appops first) and exposes it to the
// automation engine as the `lock_root` ActionHandler, gated by
// RootSafetyGate. Pulled into the rooted flavor of :app only via
// `rootedImplementation(project(":feature:lock-rooted"))`.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.lock.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
