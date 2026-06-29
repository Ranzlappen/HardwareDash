package dev.ranzlappen.gadget.feature.ambient.widget

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
import dev.ranzlappen.gadget.feature.ambient.R
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

private const val MAX_PERCENT = 100

/**
 * The ambient-light home-screen widget — a read-only consumer of the kit's
 * content/display archetype ([BaseContentWidgetProvider]). Paints the current
 * lux, a log-scale brightness bar, and the level descriptor, and opens the app
 * on tap.
 *
 * Display-only: no per-instance binding, configure activity, or pin flow. Added
 * straight from the launcher picker and self-healed to [defaultConfig] via the
 * base's `saveIfAbsent`. Repaints reactively through [AmbientWidgetController]
 * → `ContentWidgetUpdater` as the reading changes.
 */
class AmbientWidgetProvider : BaseContentWidgetProvider<AmbientWidgetConfig>() {

    override val logTag: String = "AmbientWidget"

    override fun configStore(context: Context): WidgetConfigStore<AmbientWidgetConfig> =
        entryPoint(context).ambientWidgetConfigStore()

    override fun sizePresetOf(config: AmbientWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): AmbientWidgetConfig = AmbientWidgetConfig()

    /** Tap opens the app (its launcher entry); null on the rare device without one. */
    override fun launchIntent(context: Context, appWidgetId: Int, config: AmbientWidgetConfig): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: AmbientWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val state = ep.ambientSensor().state.value
        val views = RemoteViews(context.packageName, R.layout.widget_ambient)

        val lux = state.luxLevel
        val available = state.sensorAvailable && lux != null

        views.setTextViewText(
            R.id.ambient_widget_lux,
            if (available) {
                context.getString(R.string.ambient_widget_lux_format, lux.toInt())
            } else {
                context.getString(R.string.ambient_widget_unknown)
            },
        )
        views.setProgressBar(
            R.id.ambient_widget_progress,
            MAX_PERCENT,
            if (available) AmbientBrightness.brightnessPercent(lux) else 0,
            false,
        )

        // Level / state line, hidden at the most compact density.
        views.setTextViewText(
            R.id.ambient_widget_status,
            when {
                !state.sensorAvailable -> context.getString(R.string.ambient_widget_no_sensor)
                lux == null -> context.getString(R.string.ambient_widget_unknown)
                else -> context.getString(levelLabelRes(AmbientBrightness.level(lux)))
            },
        )
        views.setViewVisibility(
            R.id.ambient_widget_status,
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
            R.id.widget_ambient_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    private fun levelLabelRes(level: AmbientBrightness.Level): Int = when (level) {
        AmbientBrightness.Level.Dark -> R.string.ambient_level_dark
        AmbientBrightness.Level.Dim -> R.string.ambient_level_dim
        AmbientBrightness.Level.Indoor -> R.string.ambient_level_indoor
        AmbientBrightness.Level.Bright -> R.string.ambient_level_bright
        AmbientBrightness.Level.Sunlight -> R.string.ambient_level_sunlight
    }

    private fun entryPoint(context: Context): AmbientWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AmbientWidgetEntryPoint::class.java,
        )

    companion object {
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = AmbientWidgetProvider::class.java
    }
}
