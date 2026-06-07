package dev.ranzlappen.gadget.feature.apps.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-tap "pin folder to home" via `AppWidgetManager.requestPinAppWidget`
 * (API 26+). The success callback targets [FolderWidgetPinReceiver] with the
 * folder id; that receiver writes the per-`appWidgetId`
 * [FolderWidgetConfig] into the kit store and repaints.
 *
 * Returns `false` when the launcher doesn't support programmatic pinning
 * (older AOSP / some third-party launchers); the caller then points the user
 * at the standard widget-tray flow (which uses the configure activity).
 */
@Singleton
class PinFolderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isSupported(): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun requestPin(folderId: Long): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, FolderWidgetProvider::class.java)

        val callbackIntent = Intent(context, FolderWidgetPinReceiver::class.java).apply {
            action = FolderWidgetPinReceiver.ACTION_PIN_CALLBACK
            putExtra(FolderWidgetPinReceiver.EXTRA_FOLDER_ID, folderId)
        }
        // The system fills in EXTRA_APPWIDGET_ID before firing the callback.
        val callback = PendingIntent.getBroadcast(
            context,
            folderId.toInt(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return manager.requestPinAppWidget(provider, null, callback)
    }
}
