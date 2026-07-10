// :feature:apps-rooted — rooted App-Organizer per-app management: pm-based
// freeze/unfreeze + am force-stop.
//
// Sibling to :feature:apps, pulled in by the rooted flavor of :app via
// `rootedImplementation(project(":feature:apps-rooted"))`. Provides
// RootedAppsRootController (bound to the AppsRootController interface owned
// by :feature:apps) and AppsRootActionHandler, bound under featureId
// "apps_root", which exposes freeze/unfreeze/force-stop to the automation
// engine.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.apps.rooted"
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:automation"))
    // The AppsRootController contract (+ result type) lives in the base
    // :feature:apps module so both flavors share one binding surface — the
    // sanctioned feature-to-feature dependency for a rooted/standard split
    // (see :feature:storage-rooted's identical pattern).
    implementation(project(":feature:apps"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
