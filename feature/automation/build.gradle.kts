// :feature:automation — privileged-automation controller contract + standard no-op (privileged intents, system-settings override, dumpsys).
//
// Screenless rooted-extras feature: the controller contract + standard no-op
// live here so shared code binds one interface in both flavors; the privileged
// impl ships in :feature:automation-rooted. Deliberately has no screen of its
// own — its capabilities are generic rooted power-user tools, not owned by
// any hardware feature, so they're exposed only via AutomationActionHandler
// (automation/) and surface as rule-builder actions in :feature:automation-ui,
// which resolves every ActionHandler from the shared registry. This resolves
// the "confusing empty twin of automation-ui" open question in the wiki: this
// module is the engine's own rooted-capability contributor, not a competing UI.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.automation"
}

dependencies {
    implementation(project(":core:automation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
