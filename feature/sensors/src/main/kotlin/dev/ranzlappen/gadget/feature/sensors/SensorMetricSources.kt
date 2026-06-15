package dev.ranzlappen.gadget.feature.sensors

import android.hardware.Sensor
import dev.ranzlappen.gadget.core.hardware.DeviceSensors
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow

/**
 * The sensors feature's readable signals — each bound
 * `@Binds @IntoMap @StringKey(METRIC_KEY)` into the shared
 * `Map<String, MetricSource>` (see [di.SensorsModule]), making every signal
 * simultaneously chartable (`:core:monitoring`), automatable (the engine's
 * `MetricThreshold` triggers), and enumerable (`:core:hardware`'s
 * `HardwareRegistry`) from this one definition. Proximity is the canonical
 * automation example: *"if proximity < 5 cm then torch off"*.
 *
 * All three are **push** sources ([MetricSource.stream] non-null when the
 * sensor exists): the OS only reports on change, so an idle signal costs
 * zero wakeups — and the engine's threshold gate wants change events, not a
 * poll cadence. On a device **without** the sensor, [MetricSource.stream]
 * returns null and [MetricSource.sample] returns the signal's absent-value
 * (0f); the evaluator's missing/false reading then fails any gate safely,
 * and the screen's capability row reports "not present".
 */
@Singleton
class ProximityMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Proximity",
        unit = "cm",
        min = 0f,
        max = sensors.maxRange(Sensor.TYPE_PROXIMITY) ?: DEFAULT_MAX_CM,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_PROXIMITY) { it[0] } ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_PROXIMITY)) {
            sensors.stream(Sensor.TYPE_PROXIMITY) { it[0] }
        } else {
            null
        }

    companion object {
        const val METRIC_KEY = "proximity"
        private const val DEFAULT_MAX_CM = 10f
    }
}

@Singleton
class LightMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Ambient light",
        unit = "lx",
        min = 0f,
        max = sensors.maxRange(Sensor.TYPE_LIGHT) ?: DEFAULT_MAX_LX,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_LIGHT) { it[0] } ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_LIGHT)) {
            sensors.stream(Sensor.TYPE_LIGHT) { it[0] }
        } else {
            null
        }

    companion object {
        const val METRIC_KEY = "light"
        private const val DEFAULT_MAX_LX = 10_000f
    }
}

@Singleton
class AccelerationMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Acceleration",
        unit = "m/s²",
        min = 0f,
        // maximumRange is per-axis; the magnitude can reach range·√3, so
        // scale the chart ceiling accordingly.
        max = (sensors.maxRange(Sensor.TYPE_ACCELEROMETER) ?: DEFAULT_MAX_AXIS) * AXIS_TO_MAGNITUDE,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_ACCELEROMETER, transform = ::magnitude) ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_ACCELEROMETER)) {
            sensors.stream(Sensor.TYPE_ACCELEROMETER, ::magnitude)
        } else {
            null
        }

    companion object {
        const val METRIC_KEY = "acceleration"
        private const val DEFAULT_MAX_AXIS = 39.2f // 4g per axis
        private const val AXIS_TO_MAGNITUDE = 1.733f // √3

        private fun magnitude(values: FloatArray): Float =
            sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }
}
