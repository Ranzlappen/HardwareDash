package dev.ranzlappen.gadget.feature.metricwidget.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.core.monitoring.MonitorChartBitmapRenderer
import dev.ranzlappen.gadget.core.monitoring.MonitorChartLayout
import dev.ranzlappen.gadget.core.monitoring.MonitorDownsampling
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseContentWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetConfig
import dev.ranzlappen.gadget.feature.metricwidget.MetricWidgetDisplay
import dev.ranzlappen.gadget.feature.metricwidget.R
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

private const val BAR_SCALE = 100

// Sparkline bitmap geometry. Fixed size (the ImageView is fitXY, so the
// resolution is stretched to the real widget size); kept well under the
// RemoteViews/Binder ~1MB transaction limit (480x132 RGB_565 ≈ 0.12 MB).
private const val SPARK_WIDTH_PX = 480
private const val SPARK_HEIGHT_PX = 132
private const val SPARK_STROKE_DP = 2f
private const val SPARK_MAX_POINTS = 120L
// Bucket-granularity hint; the metric widget doesn't own a MonitorConfig, so a
// fixed nominal sampling interval is used to size the history buckets.
private const val SPARK_NOMINAL_POLL_MS = 2_000L

/**
 * The **generic metric widget** (W4): a home-screen widget that binds to any
 * registered `MetricSource` the user picks in [MetricWidgetConfigActivity] and
 * paints its live value (+ optional bar), opening the app on tap.
 *
 * Rides the kit's content/display archetype ([BaseContentWidgetProvider]).
 * Placed from the launcher tray → the configure activity writes the bound
 * [MetricWidgetConfig]; an unbound instance (self-healed to [defaultConfig])
 * paints a neutral "pick a metric" placeholder and stays inert. Repaints
 * reactively through [MetricWidgetController] → `ContentWidgetUpdater` as the
 * bound metrics change.
 */
class MetricWidgetProvider : BaseContentWidgetProvider<MetricWidgetConfig>() {

    override val logTag: String = "MetricWidget"

    override fun configStore(context: Context): WidgetConfigStore<MetricWidgetConfig> =
        entryPoint(context).metricWidgetConfigStore()

    override fun sizePresetOf(config: MetricWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): MetricWidgetConfig = MetricWidgetConfig()

    /** Tap opens the app; inert (null) while unbound so a placeholder does nothing. */
    override fun launchIntent(context: Context, appWidgetId: Int, config: MetricWidgetConfig): Intent? =
        if (config.isBound) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        } else {
            null
        }

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: MetricWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val views = RemoteViews(context.packageName, R.layout.widget_metric)
        val source: MetricSource? = if (config.isBound) ep.metricSources()[config.metricKey] else null

        if (source == null) {
            // Unbound, or bound to a metric no longer registered — neutral placeholder.
            views.setTextViewText(
                R.id.metric_widget_value,
                context.getString(R.string.metric_widget_placeholder),
            )
            views.setViewVisibility(R.id.metric_widget_bar, View.GONE)
            views.setViewVisibility(R.id.metric_widget_unit, View.GONE)
            views.setViewVisibility(R.id.metric_widget_spark, View.GONE)
        } else {
            val descriptor = source.descriptor
            val value = source.sample()
            views.setTextViewText(R.id.metric_widget_value, formatValue(value))
            views.setTextViewText(R.id.metric_widget_unit, descriptor.unit)
            views.setViewVisibility(
                R.id.metric_widget_unit,
                if (descriptor.unit.isBlank()) View.GONE else View.VISIBLE,
            )
            if (config.tintArgb != null) {
                views.setTextColor(R.id.metric_widget_value, config.tintArgb.toInt())
            }
            val showBar = config.display == MetricWidgetDisplay.ValueAndBar &&
                density != WidgetRenderDensity.Compact
            views.setViewVisibility(R.id.metric_widget_bar, if (showBar) View.VISIBLE else View.GONE)
            if (showBar) {
                views.setProgressBar(R.id.metric_widget_bar, BAR_SCALE, barProgress(value, descriptor), false)
            }
            val showSpark = config.display == MetricWidgetDisplay.Sparkline &&
                density != WidgetRenderDensity.Compact &&
                renderSparkline(context, ep, config, descriptor, views)
            views.setViewVisibility(R.id.metric_widget_spark, if (showSpark) View.VISIBLE else View.GONE)
        }

        // Name label paints only at the Expanded density (kit convention).
        val label = config.displayName.ifBlank { source?.descriptor?.displayName.orEmpty() }
        views.setTextViewText(WidgetKitR.id.widget_label, label)
        views.setViewVisibility(
            WidgetKitR.id.widget_label,
            if (density.showLabel && config.showLabel && label.isNotBlank()) View.VISIBLE else View.GONE,
        )

        val renderer = ep.widgetAppearanceRenderer()
        renderer.applyBackground(views, config.appearance)
        if (pressed) {
            renderer.applyContentPressedFrame(context, views, config.appearance)
        }

        views.setOnClickPendingIntent(
            R.id.widget_metric_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    /**
     * Render the windowed history sparkline into [R.id.metric_widget_spark].
     * Returns `false` (so the caller hides the image) when there are fewer than
     * two history points — i.e. the metric isn't being monitored yet, the
     * documented "collecting" state.
     */
    private suspend fun renderSparkline(
        context: Context,
        ep: MetricWidgetEntryPoint,
        config: MetricWidgetConfig,
        descriptor: MetricDescriptor,
        views: RemoteViews,
    ): Boolean {
        val windowMs = config.windowSeconds.toLong() * 1_000L
        val bucketMs = MonitorDownsampling.bucketMs(windowMs, SPARK_NOMINAL_POLL_MS, SPARK_MAX_POINTS)
        val sinceMs = System.currentTimeMillis() - windowMs
        val values = ep.sampleRepository()
            .observeBucketedSince(config.metricKey, sinceMs, bucketMs)
            .first()
            .map { it.maxValue }
        if (values.size < 2) return false
        val density = context.resources.displayMetrics.density
        val bitmap = MonitorChartBitmapRenderer.render(
            values = values,
            yMax = descriptor.currentMax(),
            widthPx = SPARK_WIDTH_PX,
            heightPx = SPARK_HEIGHT_PX,
            lineColor = ContextCompat.getColor(context, R.color.metric_widget_spark_line),
            fillColor = ContextCompat.getColor(context, R.color.metric_widget_spark_fill),
            backgroundColor = ContextCompat.getColor(context, R.color.metric_widget_spark_bg),
            layout = MonitorChartLayout.Line,
            strokeWidthPx = SPARK_STROKE_DP * density,
        )
        // Intentionally NOT released to the renderer's pool: BaseContentWidgetProvider
        // owns the updateAppWidget() call (after buildRemoteViews returns), so there's
        // no safe point to release. Never releasing means the pool always allocates
        // fresh, so no in-flight bitmap is ever aliased — correct, just un-pooled.
        views.setImageViewBitmap(R.id.metric_widget_spark, bitmap)
        return true
    }

    private fun entryPoint(context: Context): MetricWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MetricWidgetEntryPoint::class.java,
        )

    companion object {
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = MetricWidgetProvider::class.java

        /** Scale [value] onto the [BAR_SCALE] track using the descriptor's live ceiling. */
        internal fun barProgress(value: Float, descriptor: MetricDescriptor): Int {
            val min = descriptor.min
            val span = (descriptor.currentMax() - min).takeIf { it > 0f } ?: return 0
            val fraction = ((value - min) / span).coerceIn(0f, 1f)
            return (fraction * BAR_SCALE).roundToInt()
        }

        /** Compact numeric format: one decimal for small magnitudes, integer otherwise. */
        internal fun formatValue(value: Float): String =
            if (abs(value) < 10f && value != value.roundToInt().toFloat()) {
                String.format("%.1f", value)
            } else {
                value.roundToInt().toString()
            }
    }
}
