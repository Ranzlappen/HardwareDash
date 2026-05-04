package com.gadget.apps.pin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gadget.widget.folder.FolderWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-tap "pin folder to home" using `AppWidgetManager.requestPinAppWidget`
 * (API 26+). The success callback PendingIntent targets [FolderWidgetProvider]
 * with [ACTION_PIN_CALLBACK] + the folder id; the provider's `onReceive`
 * writes the per-`appWidgetId` config row and triggers a paint.
 *
 * Returns `false` when the launcher doesn't support programmatic pinning
 * (older AOSP launchers, some third-party launchers); callers should show a
 * fallback hint pointing the user at the standard widget-tray flow.
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

        val callbackIntent = Intent(context, FolderWidgetProvider::class.java).apply {
            action = ACTION_PIN_CALLBACK
            putExtra(EXTRA_FOLDER_ID, folderId)
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

    companion object {
        const val ACTION_PIN_CALLBACK = "com.gadget.apps.PIN_CALLBACK"
        const val EXTRA_FOLDER_ID = "folder_id"
    }
}
