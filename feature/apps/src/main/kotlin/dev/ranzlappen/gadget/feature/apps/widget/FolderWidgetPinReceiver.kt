package dev.ranzlappen.gadget.feature.apps.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import kotlinx.coroutines.launch

/**
 * Receives the `requestPinAppWidget` success callback from [PinFolderHelper]
 * and binds the freshly-placed widget to the chosen folder: it writes the
 * per-`appWidgetId` [FolderWidgetConfig] into the kit store and repaints.
 *
 * This is the in-app pin path's analogue of the configure activity's tray-drop
 * binding — both end at a saved config the [FolderWidgetProvider] renders.
 */
class FolderWidgetPinReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PIN_CALLBACK) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || folderId < 0L) return

        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FolderWidgetEntryPoint::class.java,
        )
        val pending = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                val name = ep.appsDao().getFolder(folderId)?.name.orEmpty()
                ep.folderWidgetConfigStore().save(
                    appWidgetId,
                    FolderWidgetConfig(folderId = folderId, displayName = name),
                )
                ContentWidgetUpdater.requestUpdate(context, FolderWidgetProvider.PROVIDER_CLASS)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PIN_CALLBACK = "dev.ranzlappen.gadget.feature.apps.FOLDER_PIN_CALLBACK"
        const val EXTRA_FOLDER_ID = "folder_id"
    }
}
