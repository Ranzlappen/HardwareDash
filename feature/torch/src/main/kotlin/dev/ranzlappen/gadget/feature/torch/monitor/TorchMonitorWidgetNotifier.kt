package dev.ranzlappen.gadget.feature.torch.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.monitoring.MonitorWidgetNotifier
import dev.ranzlappen.gadget.feature.torch.widget.MonitorChartWidgetProvider
import dev.ranzlappen.gadget.feature.torch.widget.MonitorWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes a live repaint to placed monitor widgets each time a new
 * `torch_intensity` sample lands (when the metric's config has
 * `widgetEnabled = true`). The reference implementation of the
 * [MonitorWidgetNotifier] seam. Repaints both torch monitor widgets — the
 * determinate-bar [MonitorWidgetProvider] and the sparkline
 * [MonitorChartWidgetProvider]. `MonitorService` already coalesces these
 * calls, so each placed widget only needs a cheap "are there instances?"
 * check here.
 */
@Singleton
class TorchMonitorWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : MonitorWidgetNotifier {

    override val metricKey: String = TorchMetricSource.METRIC_KEY

    /** Last repaint timestamp (elapsedRealtime) — guards the throttle. */
    @Volatile
    private var lastRepaintMs: Long = 0L

    override fun onSample(value: Float) {
        // Self-throttle so a future high-rate metric reusing this notifier
        // verbatim can't pelt the launcher with RemoteViews broadcasts (each
        // one is a Binder transaction). MonitorService already coalesces, but
        // a per-notifier floor is cheap defence-in-depth. A dropped repaint is
        // harmless: each widget re-reads the full window / latest sample on
        // its next paint.
        val now = SystemClock.elapsedRealtime()
        if (now - lastRepaintMs < MIN_REPAINT_INTERVAL_MS) return
        lastRepaintMs = now
        repaint(MonitorWidgetProvider::class.java)
        repaint(MonitorChartWidgetProvider::class.java)
    }

    private fun repaint(provider: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, provider)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                this.component = component
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            },
        )
    }

    private companion object {
        /** Minimum gap between widget repaint broadcasts (ms). */
        const val MIN_REPAINT_INTERVAL_MS = 250L
    }
}
