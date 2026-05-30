package dev.ranzlappen.gadget.core.widgetkit.pin

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import kotlinx.coroutines.launch

/**
 * Reusable base for the success-callback receiver fired by
 * [android.appwidget.AppWidgetManager.requestPinAppWidget].
 *
 * When the user accepts the launcher's pin dialog, the OS fires the
 * caller-supplied success `PendingIntent`. The intent carries:
 *  - [AppWidgetManager.EXTRA_APPWIDGET_ID] — the OS-assigned ID for
 *    the new widget (added by the system).
 *  - The feature's own pending-token extra (see [tokenExtraKey] +
 *    [expectedAction]) — set by the feature's pin-request code, used
 *    by [PendingWidgetConfigs.claim] to recover the pre-pin config.
 *
 * The base claims the config, saves it under the new `appWidgetId`
 * via the feature's [WidgetConfigStore], and invokes [afterSave] so
 * the feature can trigger an immediate widget repaint (e.g. via
 * [AppWidgetManager.ACTION_APPWIDGET_UPDATE]). Each feature's
 * receiver subclass plugs in:
 *  - the feature-specific intent action + extra-key constants
 *  - logcat tag
 *  - Hilt EntryPoint accessors that hand back the per-feature
 *    [PendingWidgetConfigs] and [WidgetConfigStore]
 *  - `afterSave` for the per-feature post-save action (typically a
 *    widget-update broadcast)
 *
 * Registers in the per-feature manifest with `exported="true"` because
 * the OS fires the success callback from the system process (outside
 * the app's). The intent's explicit ComponentName routes directly to
 * the subclass — no cross-package resolution needed.
 *
 * Uses [BroadcastReceiver.goAsync] to keep the receiver alive across
 * the suspending DataStore IO (claim + save). The coroutine completes
 * within ~50 ms in practice; well under the 10-second receiver budget.
 *
 * Every step logs under the feature's [logTag] —
 * `adb logcat -s <tag>:D` traces enqueue → callback → claim → save →
 * afterSave end-to-end.
 */
abstract class BaseWidgetPinSuccessReceiver<T : WidgetKitConfig> : BroadcastReceiver() {

    /** Intent action this receiver listens for. Must match what the
     *  feature's pin-request code attached to the success callback. */
    protected abstract val expectedAction: String

    /** Intent-extra key the feature embedded the pending-config token
     *  under at pin-request time. */
    protected abstract val tokenExtraKey: String

    /** Feature's logcat tag — same string the feature passes to
     *  [PendingWidgetConfigs.tag] so the full pin flow is filterable
     *  by one tag. */
    protected abstract val logTag: String

    /** Resolve the feature's [PendingWidgetConfigs] from a Hilt
     *  EntryPoint. Implementations typically wrap
     *  `EntryPointAccessors.fromApplication(...)`. */
    protected abstract fun pendingConfigs(context: Context): PendingWidgetConfigs<T>

    /** Resolve the feature's [WidgetConfigStore]. */
    protected abstract fun configStore(context: Context): WidgetConfigStore<T>

    /**
     * Post-save hook — the feature triggers an immediate widget
     * repaint here (typically by broadcasting
     * [AppWidgetManager.ACTION_APPWIDGET_UPDATE] for [appWidgetId]).
     * Called inside the same `goAsync` coroutine as the save.
     */
    protected abstract suspend fun afterSave(context: Context, appWidgetId: Int, config: T)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(logTag, "pin-success.onReceive action=${intent.action}")
        if (intent.action != expectedAction) {
            Log.w(logTag, "action mismatch — dropping broadcast")
            return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val token = intent.getStringExtra(tokenExtraKey)
        Log.d(logTag, "extras appWidgetId=$appWidgetId token=$token")
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || token == null) {
            Log.w(logTag, "missing required extras — dropping")
            return
        }

        // claim() + save() suspend. Run the whole chain inside a single
        // goAsync() coroutine so the receiver stays alive across both
        // disk IO ops.
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                val config = pendingConfigs(context).claim(token)
                if (config == null) {
                    Log.w(logTag, "claim returned null — bailing")
                    return@launch
                }
                configStore(context).save(appWidgetId, config)
                Log.d(logTag, "save complete id=$appWidgetId")
                afterSave(context, appWidgetId, config)
                Log.d(logTag, "afterSave complete id=$appWidgetId")
            } catch (t: Throwable) {
                Log.e(logTag, "pin-success flow failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
