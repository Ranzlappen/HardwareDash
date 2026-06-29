package dev.ranzlappen.gadget.feature.flipper.monitor

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.flipper.FlipperConnectionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Push source for the connected Flipper's battery percentage (0–100). Reads the
 * charge level captured at connection time from [FlipperConnectionManager];
 * emits 0 while disconnected.
 */
@Singleton
class FlipperBatteryMetricSource @Inject constructor(
    private val manager: FlipperConnectionManager,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Flipper battery",
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = manager.state.value.toBatteryFloat()

    override fun stream(): Flow<Float> = manager.state.map { it.toBatteryFloat() }

    private fun FlipperConnectionManager.State.toBatteryFloat(): Float =
        (this as? FlipperConnectionManager.State.Connected)?.batteryPercent?.toFloat() ?: 0f

    companion object {
        const val METRIC_KEY = "flipper_battery"
    }
}
