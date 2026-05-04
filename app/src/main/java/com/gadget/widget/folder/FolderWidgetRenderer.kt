package com.gadget.widget.folder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.ui.folder.FolderPopupActivity

/**
 * Builds the [RemoteViews] for one placed folder widget. Reads the folder
 * referenced by the widget's `apps_widget_config` row and tints the layout
 * with the folder's `baseColorArgb`. Tapping anywhere on the widget root
 * starts [FolderPopupActivity] for that folder.
 *
 * Pure utility object — no Hilt dependencies — so it can be invoked from
 * either an `AppWidgetProvider.onUpdate` (broadcast context, no DI surface)
 * or an in-app explicit refresh.
 */
internal object FolderWidgetRenderer {

    suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        dao: AppsDao,
    ) {
        val config = dao.getWidgetConfig(appWidgetId) ?: run {
            // Orphaned widget (e.g. config Activity was cancelled). Show a
            // neutral placeholder so the user can long-press → reconfigure.
            appWidgetManager.updateAppWidget(appWidgetId, neutralViews(context, appWidgetId))
            return
        }
        val folder = dao.getFolder(config.folderId) ?: run {
            appWidgetManager.updateAppWidget(appWidgetId, neutralViews(context, appWidgetId))
            return
        }
        appWidgetManager.updateAppWidget(appWidgetId, viewsFor(context, appWidgetId, folder))
    }

    private fun viewsFor(context: Context, appWidgetId: Int, folder: Folder): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
        views.setTextViewText(R.id.widget_folder_name, folder.name)
        views.setTextColor(R.id.widget_folder_name, folder.baseColorArgb)
        views.setInt(R.id.widget_folder_accent, "setBackgroundColor", folder.baseColorArgb)
        views.setOnClickPendingIntent(R.id.widget_folder_root, popupPendingIntent(context, appWidgetId, folder.id))
        return views
    }

    private fun neutralViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
        views.setTextViewText(R.id.widget_folder_name, context.getString(R.string.widget_folder_label))
        // No PendingIntent — tapping does nothing until the user reconfigures.
        return views
    }

    private fun popupPendingIntent(context: Context, appWidgetId: Int, folderId: Long): PendingIntent {
        val intent = FolderPopupActivity.intent(context, folderId)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
