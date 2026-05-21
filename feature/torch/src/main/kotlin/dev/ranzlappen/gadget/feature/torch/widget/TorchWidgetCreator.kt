package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 * along through the [PendingTorchWidgetConfigs] DataStore-backed
 * bridge so the receiver can persist it once the OS assigns an
 * `appWidgetId`.
 *
 * Older launchers (`isRequestPinAppWidgetSupported() == false`)
 * cause [requestPin] to return `false` so the UI can surface an
 * error toast. Modern AOSP launchers + Pixel Launcher all support
 * the API; Samsung One UI also does on Android 8+.
 *
 * **Reliability:** the success-callback `PendingIntent` is built
 * with an *explicit* [ComponentName] pointing at
 * [WidgetPinSuccessReceiver]. Implicit (action+package) intents fail
 * silently on some OEM launchers because the OS uses different
 * resolution rules when fanning out the success callback to
 * third-party receivers. Explicit ComponentName is the canonical
 * fix and lines up with the manifest's exported intent filter.
 *
 * Logging — every step writes to logcat under tag
 * [PendingTorchWidgetConfigs.TAG] so `adb logcat -s TorchPinFlow:D`
 * traces the full flow.
 */
@Singleton
class TorchWidgetCreator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pending: PendingTorchWidgetConfigs,
) {

    /** Internal scope for the pre-pin DataStore write. Survives the
     *  synchronous return of [requestPin]. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Request the launcher to pin a new home-screen widget configured
     * per [config]. Returns `true` if the OS accepted the request
     * (the user-facing dialog opened); `false` if the launcher
     * doesn't support the pin API.
     *
     * Returning `true` does NOT mean the user accepted the dialog —
     * if they cancel, [WidgetPinSuccessReceiver] simply never fires
     * and the pending entry remains until the stale-purge janitor
     * drops it (an hour later).
     *
     * The pending config is persisted *synchronously* on the caller's
     * thread via `kotlinx.coroutines.runBlocking` so the
     * PendingIntent's token is guaranteed to resolve when the OS
     * fires the success callback. The runBlocking is acceptable
     * because the DataStore write is single-digit-ms and this method
     * is called from a Compose click handler (not the main render
     * loop).
     */
    fun requestPin(config: TorchWidgetConfig): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            Log.w(PendingTorchWidgetConfigs.TAG, "requestPin → launcher unsupported")
            return false
        }
        Log.d(PendingTorchWidgetConfigs.TAG, "requestPin → type=${config.type}")

        // Synchronous enqueue so the token persists before the OS
        // fires the success callback. The factory method is suspend
        // because DataStore writes are. Using kotlinx.coroutines'
        // runBlocking is safe here — single short write on Dispatchers.IO.
        val token = kotlinx.coroutines.runBlocking { pending.enqueue(config) }

        val provider = when (config.type) {
            WidgetType.Flashlight ->
                ComponentName(context, FlashlightWidgetProvider::class.java)
            WidgetType.Strobe ->
                ComponentName(context, StrobeWidgetProvider::class.java)
        }

        // Explicit ComponentName on the success-callback intent —
        // see the KDoc's "Reliability" note above. The OS dispatches
        // the success PendingIntent to the explicit receiver,
        // sidestepping action-only resolution flakiness on certain
        // OEM launchers.
        val successIntent = Intent(context, WidgetPinSuccessReceiver::class.java).apply {
            action = ACTION_WIDGET_PIN_SUCCESS
            component = ComponentName(context, WidgetPinSuccessReceiver::class.java)
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

        val accepted = appWidgetManager.requestPinAppWidget(
            provider,
            /* extras = */ null,
            successCallback,
        )
        Log.d(PendingTorchWidgetConfigs.TAG, "requestPin → OS accepted=$accepted")
        return accepted
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
