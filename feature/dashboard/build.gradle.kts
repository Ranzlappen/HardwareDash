// :feature:dashboard — the Home section (v3 B3).
//
// Live status cards (battery, storage, Wi-Fi, latest logbook entry), quick
// actions (torch, buzz) and the user's pinned tools. Reads features only
// through core seams — the MetricSource map, the ModuleActionRegistry and
// the LogbookDao — never a feature module.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.dashboard"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard,
    // ScreenHeader, SectionHeader, SparklineChart, Glass tokens).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination so the dashboard
    // can request top-level navigation through its onNavigate callback.
    implementation(project(":core:navigation"))
    // :core:datastore backs the persisted DashboardLayout (order / hidden /
    // pinned) the DashboardViewModel reads and writes (W9 dashboard editor).
    implementation(project(":core:datastore"))
    // :core:model — the Hilt Map<String, MetricSource> the status cards read
    // (battery / storage / Wi-Fi / torch state) without depending on features.
    implementation(project(":core:model"))
    // :core:automation — ModuleActionRegistry for the quick actions (torch
    // on/off, vibration one-shot) dispatched by featureId / actionKey.
    implementation(project(":core:automation"))
    // :core:data — LogbookDao for the "latest log entry" card.
    implementation(project(":core:data"))

    testImplementation(libs.junit)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
