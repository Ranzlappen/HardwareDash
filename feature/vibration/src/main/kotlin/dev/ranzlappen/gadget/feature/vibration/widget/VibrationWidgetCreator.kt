package dev.ranzlappen.gadget.feature.vibration.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinPolicy
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinResult
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for creating new home-screen vibration widgets from inside
 * the app. Mirror of torch's `TorchWidgetCreator`: enqueues the pre-pin
 * [VibrationWidgetConfig] into the [PendingWidgetConfigs] bridge, then calls
 * [AppWidgetManager.requestPinAppWidget] with an explicit-ComponentName
 * success callback into [WidgetPinSuccessReceiver]. Suspends so the pending
 * config is persisted before the OS can fire the callback.
 */
@Singleton
class VibrationWidgetCreator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pending: PendingWidgetConfigs<VibrationWidgetConfig>,
) {

    suspend fun requestPin(config: VibrationWidgetConfig): WidgetPinResult {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            Log.w(VibrationPinLog.TAG, "requestPin → launcher unsupported")
            return WidgetPinResult.LauncherUnsupported
        }

        // New pins always go through the designated generic provider; the
        // chosen function lives in the config's actionKey, not a provider type.
        val provider = ComponentName(context, VibrateWidgetProvider::class.java)
        val currentCount = appWidgetManager.getAppWidgetIds(provider).size
        if (!WidgetPinPolicy.canPin(currentCount)) {
            Log.w(VibrationPinLog.TAG, "requestPin → cap reached ($currentCount) action=${config.actionKey}")
            return WidgetPinResult.CapReached
        }
        Log.d(VibrationPinLog.TAG, "requestPin → action=${config.actionKey}")

        val token = pending.enqueue(config)

        val successIntent = Intent(context, WidgetPinSuccessReceiver::class.java).apply {
            action = ACTION_WIDGET_PIN_SUCCESS
            component = ComponentName(context, WidgetPinSuccessReceiver::class.java)
            putExtra(EXTRA_PENDING_CONFIG_TOKEN, token)
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            token.hashCode(),
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val accepted = appWidgetManager.requestPinAppWidget(provider, /* extras = */ null, successCallback)
        Log.d(VibrationPinLog.TAG, "requestPin → OS accepted=$accepted")
        return if (accepted) WidgetPinResult.Requested else WidgetPinResult.LauncherUnsupported
    }

    companion object {
        const val ACTION_WIDGET_PIN_SUCCESS =
            "dev.ranzlappen.gadget.feature.vibration.ACTION_WIDGET_PIN_SUCCESS"
        const val EXTRA_PENDING_CONFIG_TOKEN =
            "dev.ranzlappen.gadget.feature.vibration.EXTRA_PENDING_CONFIG_TOKEN"
    }
}
