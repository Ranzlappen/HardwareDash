package dev.ranzlappen.gadget.feature.torch.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import dev.ranzlappen.gadget.core.monitoring.MonitorConfigRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorController
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import dev.ranzlappen.gadget.feature.torch.widget.MonitorChartWidgetProvider
import dev.ranzlappen.gadget.feature.torch.widget.MonitorWidgetProvider
import dev.ranzlappen.gadget.feature.torch.widget.TorchPinLog
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torch's [BootRearmHandler]. Re-arms [MonitorController.ensureStarted]
 * iff both:
 *   1. **At least one torch monitor widget is placed** on the home
 *      screen (`AppWidgetManager.getAppWidgetIds` returns non-empty for
 *      [MonitorWidgetProvider] or [MonitorChartWidgetProvider]).
 *   2. **At least one metric is enabled** for monitoring
 *      ([MonitorConfigRepository] has a record with `enabled == true`).
 *
 * Both gates matter: starting the foreground service on boot without
 * a placed widget or an enabled metric burns battery for no user-
 * visible effect (and risks
 * `ForegroundServiceDidNotStartInTimeException` if the service has
 * nothing to bind a notification to).
 *
 * Bound into the kit's `Map<String, BootRearmHandler>` multibinding
 * via [TorchProvidesModule] under the torch feature id.
 */
@Singleton
class TorchBootRearmHandler @Inject constructor(
    private val monitorController: MonitorController,
    private val monitorConfigs: MonitorConfigRepository,
) : BootRearmHandler {

    override suspend fun onBootCompleted(context: Context) {
        if (!hasPlacedMonitorWidget(context)) {
            Log.d(TorchPinLog.TAG, "boot: no monitor widgets placed — skipping rearm")
            return
        }
        val anyEnabled = monitorConfigs
            .config(TorchMetricSource.METRIC_KEY)
            .first()
            .enabled
        if (!anyEnabled) {
            Log.d(TorchPinLog.TAG, "boot: monitoring disabled — skipping rearm")
            return
        }
        Log.d(TorchPinLog.TAG, "boot: rearming MonitorService")
        monitorController.ensureStarted()
    }

    private fun hasPlacedMonitorWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        val monitorIds = manager.getAppWidgetIds(
            ComponentName(context, MonitorWidgetProvider::class.java),
        )
        if (monitorIds.isNotEmpty()) return true
        val chartIds = manager.getAppWidgetIds(
            ComponentName(context, MonitorChartWidgetProvider::class.java),
        )
        return chartIds.isNotEmpty()
    }

    companion object {
        /** Stable feature id under which torch binds its handler into
         *  the multibinding. Doubles as the entry's logcat prefix when
         *  the kit's BootCompletedReceiver iterates the map. */
        const val FEATURE_ID: String = "torch"
    }
}
