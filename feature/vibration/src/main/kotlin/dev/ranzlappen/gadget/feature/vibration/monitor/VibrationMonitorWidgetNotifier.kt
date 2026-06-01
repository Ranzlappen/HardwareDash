package dev.ranzlappen.gadget.feature.vibration.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.monitoring.MonitorWidgetNotifier
import dev.ranzlappen.gadget.feature.vibration.widget.MonitorChartWidgetProvider
import dev.ranzlappen.gadget.feature.vibration.widget.MonitorWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes a live repaint to placed vibration monitor widgets each time a new
 * `vibration_amplitude` sample lands. Mirrors `TorchMonitorWidgetNotifier`:
 * repaints both the determinate-bar [MonitorWidgetProvider] and the sparkline
 * [MonitorChartWidgetProvider], self-throttled so a fast metric can't pelt the
 * launcher with RemoteViews broadcasts (a dropped repaint is harmless — each
 * widget re-reads on its next paint).
 */
@Singleton
class VibrationMonitorWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : MonitorWidgetNotifier {

    override val metricKey: String = VibrationMetricSource.METRIC_KEY

    @Volatile
    private var lastRepaintMs: Long = 0L

    override fun onSample(value: Float) {
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
        const val MIN_REPAINT_INTERVAL_MS = 250L
    }
}
