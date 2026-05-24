package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the success callback from
 * [android.appwidget.AppWidgetManager.requestPinAppWidget].
 *
 * Wired through [TorchWidgetCreator.requestPin]: when the user
 * accepts the launcher's pin dialog, the OS fires the supplied
 * `successCallback` PendingIntent. The intent carries:
 * - [AppWidgetManager.EXTRA_APPWIDGET_ID] — the OS-assigned ID for
 *   the new widget (added by the system).
 * - [TorchWidgetCreator.EXTRA_PENDING_CONFIG_TOKEN] — our token
 *   into [PendingTorchWidgetConfigs] (we set it ourselves at
 *   pin-request time).
 *
 * The receiver claims the config from the DataStore-backed pending
 * store, persists it under the new `appWidgetId` via
 * [TorchWidgetConfigRepository.save], then triggers an
 * [AppWidgetManager.ACTION_APPWIDGET_UPDATE] so the fresh widget
 * renders with its config immediately rather than the provider's
 * default placeholder.
 *
 * Registered in the manifest with `exported="true"` because the OS
 * fires the success callback from the system process (outside ours).
 * The intent's explicit ComponentName routes it directly here — no
 * cross-package resolution needed.
 *
 * Uses [BroadcastReceiver.goAsync] to keep the receiver alive across
 * the suspending [TorchWidgetConfigRepository.save] disk write. The
 * coroutine completes within ~50 ms in practice; well under the
 * 10-second receiver budget.
 *
 * Every step logs under [PendingTorchWidgetConfigs.TAG] —
 * `adb logcat -s TorchPinFlow:D` traces enqueue → callback → claim →
 * save → broadcastUpdate.
 */
class WidgetPinSuccessReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(PendingTorchWidgetConfigs.TAG, "WidgetPinSuccessReceiver.onReceive action=${intent.action}")
        if (intent.action != TorchWidgetCreator.ACTION_WIDGET_PIN_SUCCESS) {
            Log.w(PendingTorchWidgetConfigs.TAG, "action mismatch — dropping broadcast")
            return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val token = intent.getStringExtra(
            TorchWidgetCreator.EXTRA_PENDING_CONFIG_TOKEN,
        )
        Log.d(PendingTorchWidgetConfigs.TAG, "extras appWidgetId=$appWidgetId token=$token")
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || token == null) {
            Log.w(PendingTorchWidgetConfigs.TAG, "missing required extras — dropping")
            return
        }

        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetPinSuccessEntryPoint::class.java,
        )

        // claim() suspends now (DataStore-backed bridge). Run the
        // whole chain (claim → save → broadcast) inside a single
        // goAsync() coroutine so the receiver stays alive across
        // the disk read+write.
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val config = entry.pendingConfigs().claim(token)
                if (config == null) {
                    Log.w(PendingTorchWidgetConfigs.TAG, "claim returned null — bailing")
                    return@launch
                }
                entry.repository().save(appWidgetId, config)
                Log.d(PendingTorchWidgetConfigs.TAG, "save complete id=$appWidgetId type=${config.type}")
                broadcastTorchWidgetUpdate(context, config.type, appWidgetId)
                Log.d(PendingTorchWidgetConfigs.TAG, "broadcastUpdate sent id=$appWidgetId")
            } catch (t: Throwable) {
                Log.e(PendingTorchWidgetConfigs.TAG, "pin-success flow failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Hilt entry point — gives a system-instantiated
     *  BroadcastReceiver access to the repositories without
     *  `@AndroidEntryPoint`. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetPinSuccessEntryPoint {
        fun pendingConfigs(): PendingTorchWidgetConfigs
        fun repository(): TorchWidgetConfigRepository
    }
}
