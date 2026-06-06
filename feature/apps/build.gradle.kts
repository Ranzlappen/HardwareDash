// :feature:apps — the App-Organizer feature surface (folders, web-links,
// smart rules, the folder popup, and the folder home-screen widgets).
//
// Phase B of the folder-widgets / App-Organizer migration: the domain layer
// (scanner, repositories, launchers, favicon fetcher, icon loader, rule
// engine, biometric lock) ported out of legacy `com.gadget.apps.*` onto the
// modular `:core:data` AppsDao. Screens (Phase C), the launcher/content
// widget archetype (Phase D), and the folder widget itself (Phase E) layer on
// top.

plugins {
    id("gadget.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ranzlappen.gadget.feature.apps"

    defaultConfig {
        // Keep rules for this module's @Serializable types (FolderRule /
        // FolderRuleSet) — merged into :app's release R8 run so minified
        // builds don't strip the generated serializers.
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // :core:ui transitively brings :core:designsystem (design-system
    // components + material-icons-extended for the cover-symbol picker).
    implementation(project(":core:ui"))
    // :core:data — the App-Organizer Room layer (AppsDao + entities). The
    // sanctioned ":core:data repositories/DAO, never Room directly" path.
    implementation(project(":core:data"))
    // :core:navigation surfaces GadgetDestination.Apps + the
    // NavGraphBuilder.appsScreen() extension target (wired in Phase C).
    implementation(project(":core:navigation"))

    // androidx.core for Drawable.toBitmap (icon resolution).
    implementation(libs.androidx.core.ktx)
    // Biometric prompt for hidden/locked folders.
    implementation(libs.androidx.biometric)
    // kotlinx.serialization JSON — FolderRuleSet is @Serializable and rides
    // through RuleCodec into apps_folder_rule.rule_json.
    implementation(libs.kotlinx.serialization.json)
    // Structured logging, matching the legacy domain code being ported.
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
