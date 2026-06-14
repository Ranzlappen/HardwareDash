package dev.ranzlappen.gadget.feature.battery

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Battery metric sources bound `@IntoMap @StringKey(METRIC_KEY)` into the
 * shared `Map<String, MetricSource>` in [di.BatteryModule]. All three are
 * **push** sources backed by [BatteryMonitor.state] — no periodic polling
 * overhead when the battery isn't changing.
 */

@Singleton
class BatteryLevelMetricSource @Inject constructor(
    private val monitor: BatteryMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Battery level",
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Battery,
    )

    override suspend fun sample(): Float = monitor.state.value.level.coerceAtLeast(0).toFloat()

    override fun stream(): Flow<Float> =
        monitor.state.map { it.level.coerceAtLeast(0).toFloat() }

    companion object {
        const val METRIC_KEY = "battery_level"
    }
}

@Singleton
class BatteryTemperatureMetricSource @Inject constructor(
    private val monitor: BatteryMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Battery temperature",
        unit = "°C",
        min = 0f,
        max = 60f,
        category = MetricCategory.Battery,
    )

    override suspend fun sample(): Float = monitor.state.value.temperatureCelsius

    override fun stream(): Flow<Float> = monitor.state.map { it.temperatureCelsius }

    companion object {
        const val METRIC_KEY = "battery_temperature"
    }
}

@Singleton
class BatteryVoltageMetricSource @Inject constructor(
    private val monitor: BatteryMonitor,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Battery voltage",
        unit = "mV",
        min = 0f,
        max = 5000f,
        category = MetricCategory.Battery,
    )

    override suspend fun sample(): Float = monitor.state.value.voltageMv.toFloat()

    override fun stream(): Flow<Float> = monitor.state.map { it.voltageMv.toFloat() }

    companion object {
        const val METRIC_KEY = "battery_voltage"
    }
}
