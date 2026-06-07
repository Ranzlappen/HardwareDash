package dev.ranzlappen.gadget.feature.apps.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-tap "pin folder to home" via `AppWidgetManager.requestPinAppWidget`
 * (API 26+).
 *
 * Reliability follows the kit's proven torch pattern: the user's pre-pin
 * [FolderWidgetConfig] is persisted in the [PendingWidgetConfigs] bridge
 * **before** the request, and a token rides along in the success callback so
 * [FolderWidgetPinReceiver] can recover + save it once the OS assigns an
 * `appWidgetId`. If the OS callback never fires (a known flakiness on some OEM
 * launchers / low-RAM process death), [FolderWidgetProvider]'s
 * `reconcilePendingConfig` self-heal claims the sole pending entry instead — so
 * a freshly-pinned widget binds to its folder either way rather than stranding
 * on a blank `NO_FOLDER` default.
 *
 * [isSupported] returns `false` when the launcher doesn't support programmatic
 * pinning (older AOSP / some third-party launchers); the caller then points the
 * user at the standard widget-tray flow (the configure activity).
 */
@Singleton
class PinFolderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pending: PendingWidgetConfigs<FolderWidgetConfig>,
) {
    fun isSupported(): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /**
     * Request the launcher to pin a folder widget bound to [folder]. Returns
     * `true` if the OS accepted the request (the pin dialog opened); `false`
     * if the launcher doesn't support the pin API. Suspends because the pending
     * config must be persisted (DataStore write) before the request so the
     * success-callback token resolves when the OS later fires it.
     */
    suspend fun requestPin(folder: Folder): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, FolderWidgetProvider::class.java)

        val token = pending.enqueue(
            FolderWidgetConfig(folderId = folder.id, displayName = folder.name),
        )

        // Explicit ComponentName so the OS routes the success callback straight
        // to our receiver (action-only resolution is flaky on some OEM
        // launchers). FLAG_MUTABLE so the framework can fill in
        // EXTRA_APPWIDGET_ID — an immutable callback silently drops it, leaving
        // the receiver without an id to save under. The token carries the
        // config; the explicit component keeps the mutable PendingIntent
        // non-hijackable. FLAG_MUTABLE is ignored below API 31 (mutable by
        // default there), so minSdk 29 is unaffected.
        val callbackIntent = Intent(context, FolderWidgetPinReceiver::class.java).apply {
            action = FolderWidgetPinReceiver.ACTION_PIN_CALLBACK
            component = ComponentName(context, FolderWidgetPinReceiver::class.java)
            putExtra(FolderWidgetPinReceiver.EXTRA_PENDING_TOKEN, token)
        }
        val callback = PendingIntent.getBroadcast(
            context,
            // Hash the token so concurrent in-flight pins don't clobber each
            // other's callback PendingIntents.
            token.hashCode(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return manager.requestPinAppWidget(provider, null, callback)
    }

    companion object {
        /** Logcat tag for the folder pin flow — `adb logcat -s FolderPinFlow:D`
         *  traces enqueue → callback → claim → save end-to-end. Shared with
         *  [PendingWidgetConfigs.tag] and the receiver. */
        const val TAG = "FolderPinFlow"
    }
}
