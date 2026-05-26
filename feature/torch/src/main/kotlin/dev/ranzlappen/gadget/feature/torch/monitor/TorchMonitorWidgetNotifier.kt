package dev.ranzlappen.gadget.feature.torch.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.monitoring.MonitorWidgetNotifier
import dev.ranzlappen.gadget.feature.torch.widget.MonitorWidgetProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes a live repaint to placed monitor widgets each time a new
 * `torch_intensity` sample lands (when the metric's config has
 * `widgetEnabled = true`). The reference implementation of the
 * [MonitorWidgetNotifier] seam.
 */
@Singleton
class TorchMonitorWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : MonitorWidgetNotifier {

    override val metricKey: String = TorchMetricSource.METRIC_KEY

    override fun onSample(value: Float) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, MonitorWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(provider)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, MonitorWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                component = provider
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            },
        )
    }
}
