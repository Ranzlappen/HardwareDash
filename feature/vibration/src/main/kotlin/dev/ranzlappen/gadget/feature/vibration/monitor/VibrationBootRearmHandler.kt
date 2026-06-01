package dev.ranzlappen.gadget.feature.vibration.monitor

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import dev.ranzlappen.gadget.core.monitoring.MonitorConfigRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorController
import dev.ranzlappen.gadget.core.widgetkit.boot.BootRearmHandler
import dev.ranzlappen.gadget.feature.vibration.widget.MonitorChartWidgetProvider
import dev.ranzlappen.gadget.feature.vibration.widget.MonitorWidgetProvider
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationPinLog
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vibration's [BootRearmHandler]. Re-arms [MonitorController.ensureStarted]
 * iff both a vibration monitor widget is placed AND the `vibration_amplitude`
 * metric is enabled — so a reboot restores the monitor FGS without burning
 * battery when there's nothing to show. Mirror of `TorchBootRearmHandler`.
 *
 * Bound into the kit's `Map<String, BootRearmHandler>` multibinding via
 * [dev.ranzlappen.gadget.feature.vibration.di.VibrationModule] under the
 * vibration feature id.
 */
@Singleton
class VibrationBootRearmHandler @Inject constructor(
    private val monitorController: MonitorController,
    private val monitorConfigs: MonitorConfigRepository,
) : BootRearmHandler {

    override suspend fun onBootCompleted(context: Context) {
        if (!hasPlacedMonitorWidget(context)) {
            Log.d(VibrationPinLog.TAG, "boot: no monitor widgets placed — skipping rearm")
            return
        }
        val anyEnabled = monitorConfigs
            .config(VibrationMetricSource.METRIC_KEY)
            .first()
            .enabled
        if (!anyEnabled) {
            Log.d(VibrationPinLog.TAG, "boot: monitoring disabled — skipping rearm")
            return
        }
        Log.d(VibrationPinLog.TAG, "boot: rearming MonitorService")
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
        const val FEATURE_ID: String = "vibration"
    }
}
