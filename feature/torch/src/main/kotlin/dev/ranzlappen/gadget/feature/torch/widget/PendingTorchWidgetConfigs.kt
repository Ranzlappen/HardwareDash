package dev.ranzlappen.gadget.feature.torch.widget

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory bridge for [TorchWidgetCreator]'s pin flow.
 *
 * Problem this solves: [android.appwidget.AppWidgetManager.requestPinAppWidget]
 * doesn't return the new `appWidgetId` synchronously. Instead, the
 * OS fires the caller-supplied success [android.app.PendingIntent]
 * **after** the user accepts the launcher's pin dialog, with the
 * newly-assigned ID attached as
 * [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID].
 *
 * To carry the user's pre-pin configuration through that round-trip:
 * 1. UI calls [enqueue] before invoking `requestPinAppWidget` —
 *    receives back a stable string token.
 * 2. The success-callback `PendingIntent` carries the token in its
 *    extras (`EXTRA_PENDING_CONFIG_ID`).
 * 3. [WidgetPinSuccessReceiver.onReceive] reads the token + the
 *    `appWidgetId`, calls [claim] to pop the config, and saves it
 *    to [TorchWidgetConfigRepository] keyed by `appWidgetId`.
 *
 * Why not persistent storage? The pin flow finishes within seconds,
 * the user often cancels the launcher dialog, and a persistent store
 * would leak orphaned configs every time. The in-memory map dies
 * with the process; if the OS kills us mid-pin (rare — pin dialogs
 * are foreground), the user just re-creates the widget. Trade-off
 * intentional.
 */
@Singleton
class PendingTorchWidgetConfigs @Inject constructor() {

    private val pending = ConcurrentHashMap<String, TorchWidgetConfig>()

    /** Store a pending config; return the token to embed in the
     *  success [android.app.PendingIntent]. */
    fun enqueue(config: TorchWidgetConfig): String {
        val token = UUID.randomUUID().toString()
        pending[token] = config
        return token
    }

    /** Pop the config registered under [token]. Returns `null` if
     *  the token is unknown (already claimed, never registered, or
     *  the process was restarted between enqueue and claim). */
    fun claim(token: String): TorchWidgetConfig? = pending.remove(token)
}
