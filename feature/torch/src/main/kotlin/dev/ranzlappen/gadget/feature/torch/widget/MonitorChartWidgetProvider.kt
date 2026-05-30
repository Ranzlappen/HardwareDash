package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.data.MonitorSampleRepository
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.monitoring.MonitorChartBitmapRenderer
import dev.ranzlappen.gadget.core.monitoring.MonitorChartLayout
import dev.ranzlappen.gadget.core.monitoring.MonitorConfigRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorController
import dev.ranzlappen.gadget.core.monitoring.MonitorDownsampling
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.monitor.TorchMetricSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen monitor **chart** widget — the second torch monitoring widget,
 * alongside the determinate-bar [MonitorWidgetProvider]. It draws the windowed
 * `torch_intensity` history as a live sparkline.
 *
 * RemoteViews can't host a Compose/Vico chart, so the history is downsampled
 * and rendered to a [android.graphics.Bitmap] by the reusable
 * [MonitorChartBitmapRenderer] and shipped via `setImageViewBitmap`. The same
 * persisted [dev.ranzlappen.gadget.core.monitoring.MonitorConfig] drives both
 * widgets — `windowSeconds` sets the span and the toggle flips monitoring on/
 * off. Live updates are pushed by `MonitorService` via
 * `TorchMonitorWidgetNotifier`; the reload button forces a repaint.
 *
 * Layout uses only RemoteViews-safe (`@RemoteView`) classes (FrameLayout /
 * LinearLayout / TextView / ImageView / Button / ImageButton) — see the
 * RemoteViews note in CLAUDE.md.
 */
class MonitorChartWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        runAsync { renderAll(context, appWidgetManager, appWidgetIds) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // Re-render at the new size when the user resizes the widget.
        runAsync { renderAll(context, appWidgetManager, intArrayOf(appWidgetId)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CHART_TOGGLE -> runAsync {
                val repo = entry(context).configRepository()
                val updated = repo.get(METRIC_KEY).let { it.copy(enabled = !it.enabled) }
                repo.save(METRIC_KEY, updated)
                if (updated.enabled) entry(context).controller().ensureStarted()
                renderAllInstances(context)
            }
            ACTION_CHART_RELOAD -> runAsync { renderAllInstances(context) }
        }
    }

    private suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return
        val ep = entry(context)
        val config = ep.configRepository().get(METRIC_KEY)
        val yMax = ep.metricSources()[METRIC_KEY]?.descriptor?.max ?: DEFAULT_MAX

        val windowMs = config.windowSeconds.toLong() * 1_000L
        val bucketMs = MonitorDownsampling.bucketMs(windowMs, config.pollIntervalMs, WIDGET_MAX_POINTS)
        val sinceMs = System.currentTimeMillis() - windowMs
        val values = ep.sampleRepository()
            .observeBucketedSince(METRIC_KEY, sinceMs, bucketMs)
            .first()
            .map { it.maxValue }

        val density = context.resources.displayMetrics.density
        appWidgetIds.forEach { id ->
            val (wPx, hPx) = chartSizePx(appWidgetManager.getAppWidgetOptions(id), density)
            appWidgetManager.updateAppWidget(
                id,
                buildRemoteViews(context, values, yMax, config.chartLayout, config.enabled, wPx, hPx, density),
            )
        }
    }

    private suspend fun renderAllInstances(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MonitorChartWidgetProvider::class.java))
        renderAll(context, manager, ids)
    }

    private fun buildRemoteViews(
        context: Context,
        values: List<Float>,
        yMax: Float,
        layout: MonitorChartLayout,
        monitoringEnabled: Boolean,
        widthPx: Int,
        heightPx: Int,
        density: Float,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_monitor_chart).apply {
        setTextViewText(R.id.widget_chart_label, context.getString(R.string.widget_monitor_chart_label))
        if (values.size < 2) {
            setViewVisibility(R.id.widget_chart_image, View.GONE)
            setViewVisibility(R.id.widget_chart_empty, View.VISIBLE)
        } else {
            val bitmap = MonitorChartBitmapRenderer.render(
                values = values,
                yMax = yMax,
                widthPx = widthPx,
                heightPx = heightPx,
                lineColor = CHART_LINE_COLOR,
                fillColor = CHART_FILL_COLOR,
                layout = layout,
                strokeWidthPx = STROKE_WIDTH_DP * density,
            )
            setViewVisibility(R.id.widget_chart_empty, View.GONE)
            setViewVisibility(R.id.widget_chart_image, View.VISIBLE)
            setImageViewBitmap(R.id.widget_chart_image, bitmap)
        }
        setTextViewText(
            R.id.widget_chart_toggle,
            context.getString(
                if (monitoringEnabled) R.string.widget_monitor_toggle_on
                else R.string.widget_monitor_toggle_off,
            ),
        )
        setOnClickPendingIntent(R.id.widget_chart_toggle, actionIntent(context, ACTION_CHART_TOGGLE))
        setOnClickPendingIntent(R.id.widget_chart_reload, actionIntent(context, ACTION_CHART_RELOAD))
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "MonitorChartWidget op failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun entry(context: Context): MonitorChartWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MonitorChartWidgetEntryPoint::class.java,
        )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MonitorChartWidgetEntryPoint {
        fun sampleRepository(): MonitorSampleRepository
        fun configRepository(): MonitorConfigRepository
        fun controller(): MonitorController
        fun metricSources(): Map<String, @JvmSuppressWildcards MetricSource>
    }

    companion object {
        private const val TAG = "TorchMonitorChartWidget"
        private const val DEFAULT_MAX = 100f
        private val METRIC_KEY = TorchMetricSource.METRIC_KEY

        /** Cap on rendered points — a small widget needs far fewer than the
         *  in-app chart; bounds the bitmap work and the SQL row count. */
        private const val WIDGET_MAX_POINTS = 120L
        private const val STROKE_WIDTH_DP = 2f

        // Default render size before the launcher reports the real cells, and
        // a hard px cap so the ARGB_8888 bitmap stays well under the ~1MB
        // RemoteViews/Binder transaction limit (600x280x4 ≈ 0.67MB) even on
        // high-density displays — the sparkline is stretched to fit (fitXY),
        // so a modest resolution is imperceptible.
        private const val DEFAULT_WIDTH_DP = 250
        private const val DEFAULT_HEIGHT_DP = 110
        private const val MAX_WIDTH_PX = 600
        private const val MAX_HEIGHT_PX = 280

        // Teal accent line with a translucent fill for the Area style.
        private const val CHART_LINE_COLOR = 0xFF26C6DA.toInt()
        private const val CHART_FILL_COLOR = 0x3326C6DA

        const val ACTION_CHART_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_CHART_TOGGLE"
        const val ACTION_CHART_RELOAD =
            "dev.ranzlappen.gadget.feature.torch.ACTION_CHART_RELOAD"

        private fun chartSizePx(options: Bundle, density: Float): Pair<Int, Int> {
            val minWdp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
            val minHdp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                .takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
            val wPx = (minWdp * density).toInt().coerceIn(1, MAX_WIDTH_PX)
            val hPx = (minHdp * density).toInt().coerceIn(1, MAX_HEIGHT_PX)
            return wPx to hPx
        }

        private fun actionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MonitorChartWidgetProvider::class.java).apply {
                this.action = action
                component = ComponentName(context, MonitorChartWidgetProvider::class.java)
            }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
