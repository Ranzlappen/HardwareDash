package dev.ranzlappen.gadget.feature.apps.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseContentWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import dev.ranzlappen.gadget.feature.apps.icons.AppIconLoader
import dev.ranzlappen.gadget.feature.apps.icons.MaterialSymbol
import dev.ranzlappen.gadget.feature.apps.ui.folder.FolderPopupActivity
import dev.ranzlappen.gadget.feature.apps.widget.customization.FolderWidgetIconCatalog

/**
 * The App-Organizer folder home-screen widget — the reference consumer of the
 * kit's content/launcher archetype ([BaseContentWidgetProvider]).
 *
 * Renders a folder's cover image / cover symbol / 2x2 app-preview grid and, on
 * tap, opens the floating [FolderPopupActivity]. A single adaptive layout +
 * [WidgetRenderDensity] replaces the legacy 1x1/2x2 provider pair: the folder
 * name strip paints only at [WidgetRenderDensity.Expanded].
 *
 * Per-instance config is the kit [WidgetConfigStore]; the legacy Room
 * `apps_widget_config` table is read only by the one-time import path.
 */
class FolderWidgetProvider : BaseContentWidgetProvider<FolderWidgetConfig>() {

    override val logTag: String = "FolderWidget"

    /** Routes taps through a broadcast (so a held press frame can paint) when
     *  the user picked Flash/Pulse/Scale; None/Ripple launch the popup directly.
     *  Explicit component intent, so no manifest filter is needed. */
    override val tapAction: String = TAP_ACTION

    override fun configStore(context: Context): WidgetConfigStore<FolderWidgetConfig> =
        entryPoint(context).folderWidgetConfigStore()

    override fun sizePresetOf(config: FolderWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): FolderWidgetConfig = FolderWidgetConfig()

    /**
     * Rescue a freshly-pinned widget whose OS success callback never fired (or
     * missed after process death): claim the sole pending folder config from
     * the bridge instead of self-healing to a blank `NO_FOLDER` default. There
     * is only one folder widget kind, so the predicate just filters out any
     * stray empty entry. Defers when 2+ are pending (see [claimSolePending]).
     */
    override suspend fun reconcilePendingConfig(context: Context): FolderWidgetConfig? =
        entryPoint(context).folderPendingConfigs()
            .claimSolePending { it.folderId != FolderWidgetConfig.NO_FOLDER }

    override fun launchIntent(context: Context, appWidgetId: Int, config: FolderWidgetConfig): Intent? {
        if (config.folderId == FolderWidgetConfig.NO_FOLDER) return null
        return FolderPopupActivity.intent(context, config.folderId)
    }

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: FolderWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val dao = ep.appsDao()
        val folder = if (config.folderId != FolderWidgetConfig.NO_FOLDER) {
            dao.getFolder(config.folderId)
        } else {
            null
        }

        val views = if (folder == null) {
            neutralViews(context)
        } else {
            // Cover/name tint follows the folder's own colour unless the
            // customizer set an explicit override.
            val tint = if (config.coverTintArgb == FolderWidgetConfig.FOLLOW_FOLDER_COLOR) {
                folder.baseColorArgb
            } else {
                config.coverTintArgb.toInt()
            }
            // Widget-specific icon takes precedence over the folder's cover.
            // This lets a widget look different from the folder editor cover
            // without changing the folder itself.
            val widgetIconViews = config.iconKey?.let { key ->
                renderWidgetIcon(context, key, ep.folderWidgetIconCatalog(), tint)
            }
            if (config.showAppGridPreview) {
                renderPreviewGrid(context, folder, dao, ep.appIconLoader(), density, tint, config.showLabel)
            } else if (widgetIconViews != null) {
                widgetIconViews
            } else {
                val cover = folder.coverIcon
                when {
                    cover.startsWith("image:") -> renderCoverImage(context, folder)
                    cover.startsWith("symbol:") -> renderCoverSymbol(context, folder, tint)
                    else -> null
                } ?: renderPreviewGrid(
                    context, folder, dao, ep.appIconLoader(), density, tint, config.showLabel,
                )
            }
        }

        // Shared kit chrome (glass / solid / transparent) — identical paint
        // path as the function-driven widgets.
        ep.widgetAppearanceRenderer().applyBackground(views, config.appearance)
        // Folder-specific background override: shape / gradient / stroke via Canvas.
        // setImageViewBitmap overrides the resource set by applyBackground above.
        val folderBg = FolderWidgetBackgroundRenderer.buildBackground(config, context)
        if (folderBg != null) {
            views.setImageViewBitmap(WidgetKitR.id.widget_background, folderBg)
            views.setInt(WidgetKitR.id.widget_background, "setColorFilter", 0)
        }
        // Held tap-press frame (Flash/Pulse/Scale) painted on @id/widget_background;
        // plays concurrently with the floating popup opening behind it.
        if (pressed) {
            ep.widgetAppearanceRenderer().applyContentPressedFrame(context, views, config.appearance)
        }
        // Null PendingIntent (unbound NO_FOLDER widget) clears the click target.
        views.setOnClickPendingIntent(
            R.id.widget_folder_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    // ── Render modes ────────────────────────────────────────────────────────

    private fun renderWidgetIcon(
        context: Context,
        iconKey: String,
        catalog: FolderWidgetIconCatalog,
        tintArgb: Int,
    ): RemoteViews? {
        return when (val source = catalog.resolveSource(iconKey)) {
            is WidgetIconSource.Resource -> baseViews(context).apply {
                setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
                setViewVisibility(R.id.widget_folder_cover_symbol, View.VISIBLE)
                setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
                setImageViewResource(R.id.widget_folder_cover_symbol, source.resId)
                setInt(R.id.widget_folder_cover_symbol, "setColorFilter", tintArgb)
            }
            is WidgetIconSource.CustomFile -> {
                val bmp = runCatching { BitmapFactory.decodeFile(source.path) }.getOrNull()
                    ?: return null
                baseViews(context).apply {
                    setViewVisibility(R.id.widget_folder_cover_image, View.VISIBLE)
                    setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
                    setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
                    setImageViewBitmap(R.id.widget_folder_cover_image, bmp)
                }
            }
        }
    }

    private fun renderCoverImage(context: Context, folder: Folder): RemoteViews? {
        val path = folder.coverIcon.removePrefix("image:")
        val bmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        return baseViews(context).apply {
            setViewVisibility(R.id.widget_folder_cover_image, View.VISIBLE)
            setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
            setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
            setImageViewBitmap(R.id.widget_folder_cover_image, bmp)
        }
    }

    private fun renderCoverSymbol(context: Context, folder: Folder, tintArgb: Int): RemoteViews? {
        val symbol = MaterialSymbol.fromId(folder.coverIcon.removePrefix("symbol:")) ?: return null
        return baseViews(context).apply {
            setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
            setViewVisibility(R.id.widget_folder_cover_symbol, View.VISIBLE)
            setViewVisibility(R.id.widget_folder_grid_section, View.GONE)
            setImageViewResource(R.id.widget_folder_cover_symbol, symbol.drawableRes)
            // setColorFilter(int) defaults to SRC_IN — the right mode for a flat
            // symbol tint. The image-cover view never gets this so photos render
            // at their original colors.
            setInt(R.id.widget_folder_cover_symbol, "setColorFilter", tintArgb)
        }
    }

    private suspend fun renderPreviewGrid(
        context: Context,
        folder: Folder,
        dao: AppsDao,
        loader: AppIconLoader,
        density: WidgetRenderDensity,
        tintArgb: Int,
        showLabel: Boolean,
    ): RemoteViews {
        val views = baseViews(context).apply {
            setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
            setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
            setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)
        }
        // Paint the name strip only when the user kept it on AND the resolved
        // density is large enough to fit it.
        if (showLabel && density.showLabel) {
            views.setViewVisibility(R.id.widget_folder_name, View.VISIBLE)
            views.setTextViewText(R.id.widget_folder_name, folder.name)
            views.setTextColor(R.id.widget_folder_name, tintArgb)
        } else {
            views.setViewVisibility(R.id.widget_folder_name, View.GONE)
        }

        val records = dao.getMembership(folder.id)
            .sortedBy { it.sortOrder }
            .take(TILE_IDS.size)
            .mapNotNull { dao.getAppRecord(it.appKey) }

        for ((index, tileId) in TILE_IDS.withIndex()) {
            val record = records.getOrNull(index)
            if (record != null) {
                views.setViewVisibility(tileId, View.VISIBLE)
                views.setImageViewBitmap(tileId, loader.loadBitmap(record, sizePx = TILE_SIZE_PX))
            } else {
                views.setViewVisibility(tileId, View.INVISIBLE)
            }
        }
        return views
    }

    private fun neutralViews(context: Context): RemoteViews =
        baseViews(context).apply {
            setViewVisibility(R.id.widget_folder_cover_image, View.GONE)
            setViewVisibility(R.id.widget_folder_cover_symbol, View.GONE)
            setViewVisibility(R.id.widget_folder_grid_section, View.VISIBLE)
            setViewVisibility(R.id.widget_folder_name, View.VISIBLE)
            setTextViewText(R.id.widget_folder_name, context.getString(R.string.apps_widget_folder_label))
            for (id in TILE_IDS) setViewVisibility(id, View.INVISIBLE)
        }

    private fun baseViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_folder)

    private fun entryPoint(context: Context): FolderWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FolderWidgetEntryPoint::class.java,
        )

    companion object {
        /** This provider's class, for `getAppWidgetIds` enumeration. */
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = FolderWidgetProvider::class.java

        /** Explicit-component broadcast action a tap fires when a held press
         *  frame is configured (see [tapAction]). */
        private const val TAP_ACTION = "dev.ranzlappen.gadget.feature.apps.widget.FOLDER_WIDGET_TAP"

        private const val TILE_SIZE_PX = 96

        private val TILE_IDS = intArrayOf(
            R.id.widget_folder_tile_0,
            R.id.widget_folder_tile_1,
            R.id.widget_folder_tile_2,
            R.id.widget_folder_tile_3,
        )
    }
}
