// :core:notifications — notification-channel registry.
//
// Single seam for creating + reading + updating Android notification
// channels across the whole app. Replaces every consumer's hand-rolled
// `NotificationManagerCompat.createNotificationChannel(NotificationChannel(...))`
// dance (each one currently 5-15 lines with subtle differences in version
// guards, sound suppression, vibration suppression, importance, lazy
// creation pattern).
//
// Consumers register a ChannelSpec once at injection time; the registry
// idempotently ensures the channel exists in the system before the first
// notification is posted. Two kit consumers refactored as the proof:
// MonitorService (the determinate-progress sampling FGS) and
// WidgetFeedbackDispatcher (toast/notification widget feedback). Legacy
// :app/src/main/services consumers stay on their hand-rolled paths until
// each migrates to its own feature module.

plugins {
    id("gadget.android.library")
    id("gadget.android.hilt")
}

android {
    namespace = "dev.ranzlappen.gadget.core.notifications"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
