package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinPolicy
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinResult
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
     * **Suspend, not blocking.** The pending config must be persisted
     * *before* `requestPinAppWidget` returns so the success-callback
     * token resolves when the OS later fires it. That persistence is a
     * DataStore write (suspend), so this method suspends and the caller
     * drives it from a coroutine (`viewModelScope`). The previous
     * version blocked the UI thread with `runBlocking`; making the API
     * honestly asynchronous costs the caller one `launch { }` and
     * removes a main-thread-I/O foot-gun future modules would copy.
     *
     * Returns a [WidgetPinResult] so the caller can tell "launcher can't
     * pin" apart from "you've hit the per-kind cap" — both previously
     * collapsed into a bare `false`.
     */
    suspend fun requestPin(config: TorchWidgetConfig): WidgetPinResult {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            Log.w(PendingTorchWidgetConfigs.TAG, "requestPin → launcher unsupported")
            return WidgetPinResult.LauncherUnsupported
        }

        val provider = when (config.type) {
            WidgetType.Flashlight ->
                ComponentName(context, FlashlightWidgetProvider::class.java)
            WidgetType.Strobe ->
                ComponentName(context, StrobeWidgetProvider::class.java)
        }

        // Per-kind cap: count the currently-placed instances of this
        // provider so a user (or a pathological loop) can't pin unbounded
        // widgets and grow the per-feature DataStore without limit.
        val currentCount = appWidgetManager.getAppWidgetIds(provider).size
        if (!WidgetPinPolicy.canPin(currentCount)) {
            Log.w(PendingTorchWidgetConfigs.TAG, "requestPin → cap reached ($currentCount) type=${config.type}")
            return WidgetPinResult.CapReached
        }
        Log.d(PendingTorchWidgetConfigs.TAG, "requestPin → type=${config.type}")

        // Persist the pending config before requesting the pin so the
        // success-callback token is guaranteed to resolve when the OS
        // fires it (even across process death — the bridge is on disk).
        val token = pending.enqueue(config)

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
        // A rare `false` despite isRequestPinAppWidgetSupported == true (OEM
        // launcher quirk) surfaces the same "unsupported" message to the user.
        return if (accepted) WidgetPinResult.Requested else WidgetPinResult.LauncherUnsupported
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
