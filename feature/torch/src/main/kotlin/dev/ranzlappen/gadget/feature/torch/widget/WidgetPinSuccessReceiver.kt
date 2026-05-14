package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
 * The receiver claims the config, persists it under the new
 * `appWidgetId` via [TorchWidgetConfigRepository.save], then
 * triggers an [AppWidgetManager.ACTION_APPWIDGET_UPDATE] so the
 * fresh widget renders with its config immediately rather than the
 * provider's default placeholder.
 *
 * Registered in the manifest with `exported="true"` because the OS
 * fires the success callback from the system process (outside ours).
 * Action name is private to the package — only the
 * [TorchWidgetCreator]-built PendingIntent can target us.
 *
 * Uses [BroadcastReceiver.goAsync] to keep the receiver alive across
 * the suspending [TorchWidgetConfigRepository.save] disk write. The
 * coroutine completes within ~50 ms in practice (one Preferences
 * DataStore commit); well under the 10-second receiver budget.
 */
class WidgetPinSuccessReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TorchWidgetCreator.ACTION_WIDGET_PIN_SUCCESS) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val token = intent.getStringExtra(
            TorchWidgetCreator.EXTRA_PENDING_CONFIG_TOKEN,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || token == null) return

        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetPinSuccessEntryPoint::class.java,
        )
        val config = entry.pendingConfigs().claim(token) ?: return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                entry.repository().save(appWidgetId, config)
                broadcastUpdate(context, config.type, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun broadcastUpdate(
        context: Context,
        type: WidgetType,
        appWidgetId: Int,
    ) {
        val providerClass = when (type) {
            WidgetType.Flashlight -> FlashlightWidgetProvider::class.java
            WidgetType.Strobe -> StrobeWidgetProvider::class.java
        }
        val updateIntent = Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            context,
            providerClass,
        ).apply {
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                intArrayOf(appWidgetId),
            )
            component = ComponentName(context, providerClass)
        }
        context.sendBroadcast(updateIntent)
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
