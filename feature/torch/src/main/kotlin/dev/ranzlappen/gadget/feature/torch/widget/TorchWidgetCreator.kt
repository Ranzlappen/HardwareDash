package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for creating new home-screen torch widgets from
 * inside the app.
 *
 * Calls
 * [android.appwidget.AppWidgetManager.requestPinAppWidget] with a
 * success-callback `PendingIntent` that routes back into
 * [WidgetPinSuccessReceiver]. The pre-pin [TorchWidgetConfig] rides
 * along through the in-memory [PendingTorchWidgetConfigs] bridge so
 * the receiver can persist it once the OS assigns an `appWidgetId`.
 *
 * Older launchers (`isRequestPinAppWidgetSupported() == false`)
 * cause [requestPin] to return `false` so the UI can surface an
 * error toast. Modern AOSP launchers + Pixel Launcher all support
 * the API; Samsung One UI also does on Android 8+.
 */
@Singleton
class TorchWidgetCreator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pending: PendingTorchWidgetConfigs,
) {

    /**
     * Request the launcher to pin a new home-screen widget configured
     * per [config]. Returns `true` if the OS accepted the request
     * (the user-facing dialog opened); `false` if the launcher
     * doesn't support the pin API.
     *
     * Returning `true` does NOT mean the user accepted the dialog —
     * if they cancel, [WidgetPinSuccessReceiver] simply never fires
     * and the pending entry remains in memory until the process
     * dies (negligible — the map is bounded by user clicks per
     * session).
     */
    fun requestPin(config: TorchWidgetConfig): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false

        val token = pending.enqueue(config)

        val provider = when (config.type) {
            WidgetType.Flashlight ->
                ComponentName(context, FlashlightWidgetProvider::class.java)
            WidgetType.Strobe ->
                ComponentName(context, StrobeWidgetProvider::class.java)
        }

        val successIntent = Intent(ACTION_WIDGET_PIN_SUCCESS).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PENDING_CONFIG_TOKEN, token)
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            // requestCode uses the token's hash so concurrent in-flight
            // pin requests don't clobber each other's PendingIntents.
            token.hashCode(),
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return appWidgetManager.requestPinAppWidget(
            provider,
            /* extras = */ null,
            successCallback,
        )
    }

    companion object {
        /** Broadcast action for the success callback fired by the OS
         *  after the user accepts the pin dialog. Listened for by
         *  [WidgetPinSuccessReceiver]. */
        const val ACTION_WIDGET_PIN_SUCCESS =
            "dev.ranzlappen.gadget.feature.torch.ACTION_WIDGET_PIN_SUCCESS"

        /** Extra key carrying the [PendingTorchWidgetConfigs] token. */
        const val EXTRA_PENDING_CONFIG_TOKEN =
            "dev.ranzlappen.gadget.feature.torch.EXTRA_PENDING_CONFIG_TOKEN"
    }
}
