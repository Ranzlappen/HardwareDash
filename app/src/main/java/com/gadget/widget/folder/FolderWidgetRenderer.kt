package com.gadget.widget.folder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.apps.AppsEntryPoint
import com.gadget.apps.icons.AppIconLoader
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.ui.folder.FolderPopupActivity
import dagger.hilt.android.EntryPointAccessors

/**
 * Builds [RemoteViews] for one placed folder widget.
 *
 * - Looks up the folder via the per-`appWidgetId` config row.
 * - Tints the folder name with the folder's `baseColorArgb`.
 * - Loads up to 4 member-app icons via [AppIconLoader] and pins them into
 *   the four tile `ImageView` slots; unused slots are set to INVISIBLE so
 *   the 2x2 grid alignment stays stable.
 * - Wires the widget root's tap → `FolderPopupActivity`.
 *
 * Suspend because [AppIconLoader] does I/O. Callers (the provider's
 * onUpdate, FolderWidgetController, the config Activity's post-pick paint)
 * are already in coroutines.
 */
internal object FolderWidgetRenderer {

    private val tileIds = intArrayOf(
        R.id.widget_folder_tile_0,
        R.id.widget_folder_tile_1,
        R.id.widget_folder_tile_2,
        R.id.widget_folder_tile_3,
    )

    suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        dao: AppsDao,
    ) {
        val config = dao.getWidgetConfig(appWidgetId) ?: run {
            // Orphaned widget (config Activity was cancelled) — paint a
            // neutral placeholder so the user can long-press → reconfigure.
            appWidgetManager.updateAppWidget(appWidgetId, neutralViews(context))
            return
        }
        val folder = dao.getFolder(config.folderId) ?: run {
            appWidgetManager.updateAppWidget(appWidgetId, neutralViews(context))
            return
        }

        val members = dao.getMembership(folder.id)
            .sortedBy { it.sortOrder }
            .take(tileIds.size)
        val records = members.mapNotNull { dao.getAppRecord(it.appKey) }

        val loader = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appIconLoader()

        appWidgetManager.updateAppWidget(
            appWidgetId,
            previewGridViews(context, appWidgetId, folder, records, loader),
        )
    }

    private suspend fun previewGridViews(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        records: List<AppRecord>,
        loader: AppIconLoader,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
        // Mode 2: hide cover, show grid section.
        views.setViewVisibility(R.id.widget_folder_cover, View.GONE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)

        views.setTextViewText(R.id.widget_folder_name, folder.name)
        views.setTextColor(R.id.widget_folder_name, folder.baseColorArgb)

        for ((index, tileId) in tileIds.withIndex()) {
            val record = records.getOrNull(index)
            if (record != null) {
                views.setViewVisibility(tileId, View.VISIBLE)
                val bitmap = loader.loadBitmap(record, sizePx = TILE_SIZE_PX)
                views.setImageViewBitmap(tileId, bitmap)
            } else {
                // Preserve the 2x2 layout slot, just don't paint a tile.
                views.setViewVisibility(tileId, View.INVISIBLE)
            }
        }

        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    private fun neutralViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
        views.setViewVisibility(R.id.widget_folder_cover, View.GONE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_folder_name,
            context.getString(R.string.widget_folder_label),
        )
        for (id in tileIds) views.setViewVisibility(id, View.INVISIBLE)
        // No PendingIntent — taps are no-ops until the user reconfigures via
        // long-press on the home screen.
        return views
    }

    private fun popupPendingIntent(
        context: Context,
        appWidgetId: Int,
        folderId: Long,
    ): PendingIntent {
        val intent = FolderPopupActivity.intent(context, folderId)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val TILE_SIZE_PX = 96
}
