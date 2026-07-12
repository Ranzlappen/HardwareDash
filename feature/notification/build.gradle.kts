// :feature:notification — notification controller contract + standard no-op (sticky override, listener access, lock-screen overlay), plus the full screen (channel inspector, notification builder, active-notifications monitoring, automation).
//
// The controller contract + standard no-op live here so shared code binds one
// interface in both flavors; the privileged impl ships in
// :feature:notification-rooted. The screen renders the standard-flavor
// channel inspector / builder (post + cancel a test notification, no root)
// plus a rooted-only panel (sticky-channel-importance override, listener-
// access grant, bounded lock-screen overlay test, reset-all) that calls
// straight through the shared NotificationController seam — never branching
// on BuildConfig.IS_ROOTED. This module also owns the real
// NotificationListenerService (declared + manifest-registered here) backing
// the `active_notifications` MetricSource.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.notification"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard, GadgetChip,
    // GadgetTextField, the design-system component library).
    implementation(project(":core:ui"))
    // :core:monitoring — the reusable monitoring container + MetricSource
    // seam (api-exposes :core:ui and :core:model). Backs `active_notifications`.
    implementation(project(":core:monitoring"))
    // :core:automation — the action contract (ModuleAction / ActionHandler).
    implementation(project(":core:automation"))
    // :core:navigation surfaces GadgetDestination.Notification + the
    // NavGraphBuilder.notificationScreen() extension target.
    implementation(project(":core:navigation"))
    // :core:root — RootCapabilityRegistry (isRootedFlavor) drives whether the
    // rooted panel renders; the screen never branches on BuildConfig.IS_ROOTED.
    implementation(project(":core:root"))
    // :core:notifications — NotificationChannelRegistry (idempotent
    // channel creation) backs the builder's three importance-tier test
    // channels; never a hand-rolled createNotificationChannel() dance.
    implementation(project(":core:notifications"))
    // :core:permissions — this feature owns the NotificationListenerService,
    // so it contributes a FeaturePermissions group (@IntoMap) surfacing the
    // notification-listener special permission (not in the app baseline) in
    // the centralized Permissions dashboard (W5).
    implementation(project(":core:permissions"))
    // androidx.core for NotificationCompat (the in-app test-notification
    // builder) + POST_NOTIFICATIONS-safe posting helpers.
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
}
