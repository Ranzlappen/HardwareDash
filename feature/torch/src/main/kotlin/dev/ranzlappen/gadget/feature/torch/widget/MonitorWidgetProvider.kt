package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.core.monitoring.MonitorConfigRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorController
import dev.ranzlappen.gadget.core.data.MonitorSampleRepository
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.monitor.TorchMetricSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Home-screen monitor widget — torch's reference implementation of
 * "show as widget" for the reusable monitoring framework.
 *
 * Renders the latest `torch_intensity` reading as a determinate
 * [android.widget.ProgressBar] plus a toggle (start/stop monitoring) and
 * a reload button. Add it from the launcher's widget picker. Live updates
 * are pushed by [dev.ranzlappen.gadget.core.monitoring.MonitorService]
 * via [TorchMonitorWidgetNotifier] whenever the metric's config has
 * `widgetEnabled = true`; the reload button forces a repaint otherwise.
 *
 * Layout uses only RemoteViews-safe (`@RemoteView`) classes
 * (FrameLayout / LinearLayout / TextView / Button / ImageButton /
 * ProgressBar) — see the RemoteViews note in CLAUDE.md.
 */
class MonitorWidgetProvider : AppWidgetProvider() {

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
        // Re-render on resize so the determinate bar repaints at the new
        // size immediately, matching MonitorChartWidgetProvider (previously
        // the bar widget only refreshed on the next sample).
        runAsync { renderAll(context, appWidgetManager, intArrayOf(appWidgetId)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MONITOR_TOGGLE -> runAsync {
                val repo = entry(context).configRepository()
                val updated = repo.get(METRIC_KEY).let { it.copy(enabled = !it.enabled) }
                repo.save(METRIC_KEY, updated)
                if (updated.enabled) entry(context).controller().ensureStarted()
                renderAllInstances(context)
            }
            ACTION_MONITOR_RELOAD -> runAsync { renderAllInstances(context) }
        }
    }

    private suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return
        val ep = entry(context)
        val value = ep.sampleRepository().observeLatest(METRIC_KEY).first()?.value ?: 0f
        val enabled = ep.configRepository().get(METRIC_KEY).enabled
        val max = ep.metricSources()[METRIC_KEY]?.descriptor?.currentMax() ?: DEFAULT_MAX
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, value, max, enabled))
        }
    }

    private suspend fun renderAllInstances(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MonitorWidgetProvider::class.java))
        renderAll(context, manager, ids)
    }

    private fun buildRemoteViews(
        context: Context,
        value: Float,
        max: Float,
        monitoringEnabled: Boolean,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_monitor).apply {
        val ceiling = max.roundToInt().coerceAtLeast(1)
        val percent = value.roundToInt().coerceIn(0, ceiling)
        setTextViewText(
            R.id.widget_monitor_label,
            context.getString(R.string.widget_monitor_value, percent),
        )
        setProgressBar(R.id.widget_monitor_bar, ceiling, percent, false)
        setTextViewText(
            R.id.widget_monitor_toggle,
            context.getString(
                if (monitoringEnabled) R.string.widget_monitor_toggle_on
                else R.string.widget_monitor_toggle_off,
            ),
        )
        setOnClickPendingIntent(R.id.widget_monitor_toggle, actionIntent(context, ACTION_MONITOR_TOGGLE))
        setOnClickPendingIntent(R.id.widget_monitor_reload, actionIntent(context, ACTION_MONITOR_RELOAD))
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "MonitorWidget op failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun entry(context: Context): MonitorWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MonitorWidgetEntryPoint::class.java,
        )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MonitorWidgetEntryPoint {
        fun sampleRepository(): MonitorSampleRepository
        fun configRepository(): MonitorConfigRepository
        fun controller(): MonitorController
        fun metricSources(): Map<String, @JvmSuppressWildcards MetricSource>
    }

    companion object {
        private const val TAG = "TorchMonitorWidget"
        private const val DEFAULT_MAX = 100f
        private val METRIC_KEY = TorchMetricSource.METRIC_KEY

        const val ACTION_MONITOR_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_MONITOR_TOGGLE"
        const val ACTION_MONITOR_RELOAD =
            "dev.ranzlappen.gadget.feature.torch.ACTION_MONITOR_RELOAD"

        private fun actionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MonitorWidgetProvider::class.java).apply {
                this.action = action
                component = ComponentName(context, MonitorWidgetProvider::class.java)
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
