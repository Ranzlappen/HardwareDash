package dev.ranzlappen.gadget.feature.notification.listener

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-lifetime bridge between [GadgetNotificationListenerService] (which
 * only the OS can construct, on its own binder callback thread) and the rest
 * of the app (the `active_notifications` [dev.ranzlappen.gadget.core.model.MetricSource]
 * and the screen's capability row).
 *
 * `@Singleton` so both the Hilt-injected service and the metric source share
 * the same instance. There is exactly one listener service per app, so a
 * single shared bridge is correct — no per-connection keying needed.
 */
@Singleton
class ActiveNotificationsBridge @Inject constructor() {

    private val _activeCount = MutableStateFlow(0)

    /** Count of currently active (posted, not yet cleared) notifications
     *  from the last [GadgetNotificationListenerService] callback. `0` when
     *  the listener has never connected — indistinguishable from "genuinely
     *  zero notifications", which is an acceptable simplification: the
     *  screen's capability row separately reports [listenerConnected] so the
     *  two states aren't conflated for the user. */
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val _listenerConnected = MutableStateFlow(false)

    /** Whether the OS currently has [GadgetNotificationListenerService]
     *  bound. Flips true on `onListenerConnected`, false on
     *  `onListenerDisconnected` (access revoked, service killed, etc). */
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    fun onListenerConnected() {
        _listenerConnected.value = true
    }

    fun onListenerDisconnected() {
        _listenerConnected.value = false
        _activeCount.value = 0
    }

    fun updateActiveCount(count: Int) {
        _activeCount.value = count
    }
}
