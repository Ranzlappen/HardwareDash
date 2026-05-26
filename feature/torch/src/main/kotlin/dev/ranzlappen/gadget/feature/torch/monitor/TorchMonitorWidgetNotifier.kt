package dev.ranzlappen.gadget.feature.torch.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    override fun onSample(value: Float) {
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
}
