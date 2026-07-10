package dev.ranzlappen.gadget.feature.notification

import androidx.compose.runtime.Immutable

/**
 * Stateless view-state consumed by [NotificationScreenContent].
 *
 * Produced by [NotificationViewModel.state] from: the persisted builder
 * draft, a live re-read of [android.app.NotificationManager.getNotificationChannels],
 * [dev.ranzlappen.gadget.feature.notification.listener.ActiveNotificationsBridge]
 * (listener-connected + active count), and
 * [dev.ranzlappen.gadget.core.root.RootCapabilityRegistry.isRootedFlavor].
 *
 * `@Immutable` so Compose skips recompositions when the structural value is
 * unchanged across emissions.
 */
@Immutable
data class NotificationScreenState(
    val isRootedFlavor: Boolean = false,
    val channels: List<NotificationChannelSummary> = emptyList(),
    val listenerConnected: Boolean = false,
    val activeNotificationCount: Int = 0,
    val builderTitle: String = "",
    val builderBody: String = "",
    val builderImportance: NotificationImportance = NotificationImportance.Default,
    val lastPostedNotificationId: Int? = null,
    val stickyChannelId: String = "",
    val overlayMessage: String = "",
    val overlayDurationMillis: Long = DEFAULT_OVERLAY_DURATION_MILLIS,
) {
    companion object {
        val Initial = NotificationScreenState()

        const val DEFAULT_OVERLAY_DURATION_MILLIS = 5_000L
    }
}
