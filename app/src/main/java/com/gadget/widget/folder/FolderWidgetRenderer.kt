package com.gadget.widget.folder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.apps.AppsEntryPoint
import com.gadget.apps.icons.AppIconLoader
import com.gadget.apps.icons.MaterialSymbol
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.ui.folder.FolderPopupActivity
import dagger.hilt.android.EntryPointAccessors

/**
 * Builds [RemoteViews] for one placed folder widget.
 *
 * Picks the layout based on the per-`appWidgetId` config row's `sizeVariant`:
 *  - [SIZE_1X1] → `widget_folder_1x1`: cover icon when set, else a tinted
 *                 folder symbol filling the cell. No name, no tile grid.
 *  - [SIZE_2X2] (default) → `widget_folder_2x2`: cover icon when set, else
 *                 the folder name + a 2×2 grid of preview app icons.
 *
 * Suspend because [AppIconLoader] does I/O. Callers (the providers' onUpdate,
 * `FolderWidgetController`, the config Activity's post-pick paint) are
 * already in coroutines.
 */
internal object FolderWidgetRenderer {

    const val SIZE_1X1 = "1x1"
    const val SIZE_2X2 = "2x2"

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
            appWidgetManager.updateAppWidget(appWidgetId, neutralViews(context, SIZE_2X2))
            return
        }
        val folder = dao.getFolder(config.folderId) ?: run {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                neutralViews(context, config.sizeVariant),
            )
            return
        }

        val views = when (config.sizeVariant) {
            SIZE_1X1 -> render1x1(context, appWidgetId, folder)
            else -> render2x2(context, appWidgetId, folder, dao)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // ── 1x1 ─────────────────────────────────────────────────────────────────

    private fun render1x1(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_1x1)
        val cover = folder.coverIcon
        when {
            cover.startsWith("image:") -> {
                val bmp = runCatching {
                    BitmapFactory.decodeFile(cover.removePrefix("image:"))
                }.getOrNull()
                if (bmp != null) {
                    views.setViewVisibility(R.id.widget_folder_cover, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_folder_default, View.GONE)
                    views.setImageViewBitmap(R.id.widget_folder_cover, bmp)
                } else {
                    show1x1Default(views, folder)
                }
            }
            cover.startsWith("symbol:") -> {
                val sym = MaterialSymbol.fromId(cover.removePrefix("symbol:"))
                if (sym != null) {
                    views.setViewVisibility(R.id.widget_folder_cover, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_folder_default, View.GONE)
                    views.setImageViewResource(R.id.widget_folder_cover, sym.drawableRes)
                    views.setInt(
                        R.id.widget_folder_cover,
                        "setColorFilter",
                        folder.baseColorArgb,
                    )
                } else {
                    show1x1Default(views, folder)
                }
            }
            else -> show1x1Default(views, folder)
        }
        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    private fun show1x1Default(views: RemoteViews, folder: Folder) {
        views.setViewVisibility(R.id.widget_folder_cover, View.GONE)
        views.setViewVisibility(R.id.widget_folder_default, View.VISIBLE)
        views.setInt(R.id.widget_folder_default, "setColorFilter", folder.baseColorArgb)
    }

    // ── 2x2 ─────────────────────────────────────────────────────────────────

    private suspend fun render2x2(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        dao: AppsDao,
    ): RemoteViews {
        val coverViews = coverViews2x2OrNull(context, appWidgetId, folder)
        if (coverViews != null) return coverViews

        val members = dao.getMembership(folder.id)
            .sortedBy { it.sortOrder }
            .take(tileIds.size)
        val records = members.mapNotNull { dao.getAppRecord(it.appKey) }

        val loader = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appIconLoader()

        return previewGridViews(context, appWidgetId, folder, records, loader)
    }

    private fun coverViews2x2OrNull(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
    ): RemoteViews? {
        val cover = folder.coverIcon
        return when {
            cover.startsWith("image:") -> {
                val path = cover.removePrefix("image:")
                val bmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                    ?: return null
                build2x2CoverViews(context, appWidgetId, folder) { views ->
                    views.setImageViewBitmap(R.id.widget_folder_cover, bmp)
                }
            }
            cover.startsWith("symbol:") -> {
                val sym = MaterialSymbol.fromId(cover.removePrefix("symbol:"))
                    ?: return null
                build2x2CoverViews(context, appWidgetId, folder) { views ->
                    views.setImageViewResource(R.id.widget_folder_cover, sym.drawableRes)
                    views.setInt(
                        R.id.widget_folder_cover,
                        "setColorFilter",
                        folder.baseColorArgb,
                    )
                }
            }
            else -> null
        }
    }

    private fun build2x2CoverViews(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        configure: (RemoteViews) -> Unit,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
        views.setViewVisibility(R.id.widget_folder_cover, View.VISIBLE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
        configure(views)
        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    private suspend fun previewGridViews(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        records: List<AppRecord>,
        loader: AppIconLoader,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_folder_2x2)
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
                views.setViewVisibility(tileId, View.INVISIBLE)
            }
        }

        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    // ── Shared ──────────────────────────────────────────────────────────────

    private fun neutralViews(context: Context, sizeVariant: String): RemoteViews =
        when (sizeVariant) {
            SIZE_1X1 -> RemoteViews(context.packageName, R.layout.widget_folder_1x1).also {
                it.setViewVisibility(R.id.widget_folder_cover, View.GONE)
                it.setViewVisibility(R.id.widget_folder_default, View.VISIBLE)
            }
            else -> RemoteViews(context.packageName, R.layout.widget_folder_2x2).also {
                it.setViewVisibility(R.id.widget_folder_cover, View.GONE)
                it.setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)
                it.setTextViewText(
                    R.id.widget_folder_name,
                    context.getString(R.string.widget_folder_label),
                )
                for (id in tileIds) it.setViewVisibility(id, View.INVISIBLE)
            }
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
