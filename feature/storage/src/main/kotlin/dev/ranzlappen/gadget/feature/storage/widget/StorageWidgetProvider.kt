package dev.ranzlappen.gadget.feature.storage.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseContentWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.storage.R
import dev.ranzlappen.gadget.feature.storage.toDisplayGb
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

private const val MAX_PERCENT = 100

/**
 * The internal-storage status home-screen widget — a read-only consumer of the
 * kit's content/display archetype ([BaseContentWidgetProvider]). Paints the
 * used %, a progress bar, and the free-space line, and opens the app on tap.
 *
 * Display-only: no per-instance binding, configure activity, or pin flow. Added
 * straight from the launcher picker and self-healed to [defaultConfig] via the
 * base's `saveIfAbsent`. Repaints reactively through [StorageWidgetController]
 * → `ContentWidgetUpdater` as usage changes.
 */
class StorageWidgetProvider : BaseContentWidgetProvider<StorageWidgetConfig>() {

    override val logTag: String = "StorageWidget"

    override fun configStore(context: Context): WidgetConfigStore<StorageWidgetConfig> =
        entryPoint(context).storageWidgetConfigStore()

    override fun sizePresetOf(config: StorageWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): StorageWidgetConfig = StorageWidgetConfig()

    /** Tap opens the app (its launcher entry); null on the rare device without one. */
    override fun launchIntent(context: Context, appWidgetId: Int, config: StorageWidgetConfig): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: StorageWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val monitor = ep.storageMonitor()
        val usedPercent = monitor.internalUsedPercent().toInt().coerceIn(0, MAX_PERCENT)
        val freeBytes = monitor.internalFreeBytes()
        val views = RemoteViews(context.packageName, R.layout.widget_storage)

        views.setTextViewText(
            R.id.storage_widget_percent,
            context.getString(R.string.storage_widget_percent_format, usedPercent),
        )
        views.setProgressBar(R.id.storage_widget_progress, MAX_PERCENT, usedPercent, false)

        // Free-space line hidden at the most compact density to keep the % legible.
        views.setTextViewText(
            R.id.storage_widget_free,
            context.getString(R.string.storage_widget_free_format, freeBytes.toDisplayGb()),
        )
        views.setViewVisibility(
            R.id.storage_widget_free,
            if (density == WidgetRenderDensity.Compact) View.GONE else View.VISIBLE,
        )

        // Name label paints only at the Expanded density (kit convention).
        views.setTextViewText(WidgetKitR.id.widget_label, config.displayName)
        views.setViewVisibility(
            WidgetKitR.id.widget_label,
            if (density.showLabel) View.VISIBLE else View.GONE,
        )

        val renderer = ep.widgetAppearanceRenderer()
        renderer.applyBackground(views, config.appearance)
        if (pressed) {
            renderer.applyContentPressedFrame(context, views, config.appearance)
        }

        views.setOnClickPendingIntent(
            R.id.widget_storage_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    private fun entryPoint(context: Context): StorageWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            StorageWidgetEntryPoint::class.java,
        )

    companion object {
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = StorageWidgetProvider::class.java
    }
}
