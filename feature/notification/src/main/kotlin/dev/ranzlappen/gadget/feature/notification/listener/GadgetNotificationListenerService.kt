package dev.ranzlappen.gadget.feature.notification.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The real notification-listener component the screen's "grant listener
 * access" capability unlocks — see the module's Manifest for the
 * `BIND_NOTIFICATION_LISTENER_SERVICE` declaration.
 *
 * Two independent grant paths bind this same component:
 *  - **Standard flavor**: the user manually enables it from the system
 *    "Notification access" settings screen (`ACTION_NOTIFICATION_LISTENER_SETTINGS`,
 *    launched by the screen's capability-row action).
 *  - **Rooted flavor**: `RootedNotificationController.grantListenerAccess()`
 *    (`cmd notification allow_listener`) grants it programmatically, no user
 *    tap required — see `:feature:notification-rooted`'s `ListenerAccessHelper`.
 *
 * Once bound, every `onNotificationPosted` / `onNotificationRemoved`
 * callback refreshes [ActiveNotificationsBridge.activeCount] from
 * [getActiveNotifications], the source the `active_notifications`
 * [dev.ranzlappen.gadget.core.model.MetricSource] reads. `@AndroidEntryPoint`
 * on a `Service` subclass is a supported Hilt injection site (mirrors
 * `StrobeService` / `VibrationPlaybackService` elsewhere in this codebase).
 */
@AndroidEntryPoint
class GadgetNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var bridge: ActiveNotificationsBridge

    override fun onListenerConnected() {
        super.onListenerConnected()
        bridge.onListenerConnected()
        refreshActiveCount()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        bridge.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refreshActiveCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshActiveCount()
    }

    /** [getActiveNotifications] can throw if the binder call races a
     *  just-revoked grant; degrade to "unchanged" rather than crash the
     *  listener process. */
    private fun refreshActiveCount() {
        val count = runCatching { activeNotifications?.size ?: 0 }.getOrNull() ?: return
        bridge.updateActiveCount(count)
    }
}
