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
 *  - [SIZE_1X1] → `widget_folder_1x1`: cover icon when set, else a 2×2
 *                 mini-tile grid of preview app icons (no name strip).
 *  - [SIZE_2X2] (default) → `widget_folder_2x2`: cover icon when set, else
 *                 the folder name + a 2×2 grid of preview app icons.
 *
 * Both layouts share the same view IDs (cover_image / cover_symbol /
 * grid_section / tile_0..3), so the renderer treats them uniformly. The 2×2
 * adds a `widget_folder_name` TextView the 1×1 doesn't have; renderers gate
 * setting that text on whether the layout includes it.
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
        appWidgetManager.updateAppWidget(
            appWidgetId,
            renderFor(context, appWidgetId, folder, dao, config.sizeVariant),
        )
    }

    private suspend fun renderFor(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        dao: AppsDao,
        sizeVariant: String,
    ): RemoteViews {
        val layoutRes = layoutFor(sizeVariant)
        val cover = folder.coverIcon
        return when {
            cover.startsWith("image:") -> renderCoverImage(context, appWidgetId, folder, layoutRes)
                ?: renderPreviewGrid(context, appWidgetId, folder, dao, sizeVariant)
            cover.startsWith("symbol:") -> renderCoverSymbol(context, appWidgetId, folder, layoutRes)
                ?: renderPreviewGrid(context, appWidgetId, folder, dao, sizeVariant)
            else -> renderPreviewGrid(context, appWidgetId, folder, dao, sizeVariant)
        }
    }

    // ── Cover (image / symbol) — shared between 1x1 and 2x2 ─────────────────

    private fun renderCoverImage(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        layoutRes: Int,
    ): RemoteViews? {
        val path = folder.coverIcon.removePrefix("image:")
        val bmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        val views = RemoteViews(context.packageName, layoutRes)
        views.setViewVisibility(R.id.widget_folder_cover_image, View.VISIBLE)
        views.setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
        views.setImageViewBitmap(R.id.widget_folder_cover_image, bmp)
        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    private fun renderCoverSymbol(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        layoutRes: Int,
    ): RemoteViews? {
        val sym = MaterialSymbol.fromId(folder.coverIcon.removePrefix("symbol:")) ?: return null
        val views = RemoteViews(context.packageName, layoutRes)
        views.setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
        views.setViewVisibility(R.id.widget_folder_cover_symbol, View.VISIBLE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
        views.setImageViewResource(R.id.widget_folder_cover_symbol, sym.drawableRes)
        // ImageView.setColorFilter(int) defaults to SRC_IN, the right mode for
        // a flat symbol tint. The image-cover view above never gets this so
        // user-uploaded photos render at their original colors.
        views.setInt(
            R.id.widget_folder_cover_symbol,
            "setColorFilter",
            folder.baseColorArgb,
        )
        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            popupPendingIntent(context, appWidgetId, folder.id),
        )
        return views
    }

    // ── Preview-tile grid (default mode for both 1x1 and 2x2) ───────────────

    private suspend fun renderPreviewGrid(
        context: Context,
        appWidgetId: Int,
        folder: Folder,
        dao: AppsDao,
        sizeVariant: String,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(sizeVariant))
        views.setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
        views.setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)

        // 2×2 has a name strip; 1×1 doesn't. Setting text on a missing view is
        // a no-op on RemoteViews but we guard anyway for clarity.
        if (sizeVariant != SIZE_1X1) {
            views.setTextViewText(R.id.widget_folder_name, folder.name)
            views.setTextColor(R.id.widget_folder_name, folder.baseColorArgb)
        }

        val members = dao.getMembership(folder.id)
            .sortedBy { it.sortOrder }
            .take(tileIds.size)
        val records = members.mapNotNull { dao.getAppRecord(it.appKey) }
        val loader = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appIconLoader()

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

    private fun neutralViews(context: Context, sizeVariant: String): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(sizeVariant))
        views.setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
        views.setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
        views.setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)
        if (sizeVariant != SIZE_1X1) {
            views.setTextViewText(
                R.id.widget_folder_name,
                context.getString(R.string.widget_folder_label),
            )
        }
        for (id in tileIds) views.setViewVisibility(id, View.INVISIBLE)
        return views
    }

    private fun layoutFor(sizeVariant: String): Int = when (sizeVariant) {
        SIZE_1X1 -> R.layout.widget_folder_1x1
        else -> R.layout.widget_folder_2x2
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
