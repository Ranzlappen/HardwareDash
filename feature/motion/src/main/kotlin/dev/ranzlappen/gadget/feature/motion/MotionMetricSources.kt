package dev.ranzlappen.gadget.feature.motion

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
 * The motion feature's readable signals — each bound
 * `@Binds @IntoMap @StringKey(METRIC_KEY)` into the shared
 * `Map<String, MetricSource>` (see [di.MotionModule]), making every signal
 * simultaneously chartable (`:core:monitoring`), automatable (the engine's
 * `MetricThreshold` triggers), and enumerable (`:core:hardware`'s
 * `HardwareRegistry`) from this one definition.
 *
 * All three are **push** sources ([MetricSource.stream] non-null when the
 * sensor exists): the OS only reports on change, so an idle signal costs
 * zero wakeups. On a device **without** the sensor, [MetricSource.stream]
 * returns null and [MetricSource.sample] returns the signal's absent-value
 * (0f); the evaluator's missing/false reading then fails any gate safely,
 * and the screen's capability row reports "not present".
 */
@Singleton
class RotationRateMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Rotation rate",
        unit = "rad/s",
        min = 0f,
        max = DEFAULT_MAX_RAD_S,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_GYROSCOPE, transform = ::magnitude) ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_GYROSCOPE)) {
            sensors.stream(Sensor.TYPE_GYROSCOPE, ::magnitude)
        } else null

    companion object {
        const val METRIC_KEY = "rotation_rate"
        private const val DEFAULT_MAX_RAD_S = 10f
        private fun magnitude(values: FloatArray): Float =
            sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }
}

@Singleton
class StepCounterMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Step counter",
        unit = "steps",
        min = 0f,
        max = Float.MAX_VALUE,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_STEP_COUNTER) { it[0] } ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_STEP_COUNTER)) {
            sensors.stream(Sensor.TYPE_STEP_COUNTER) { it[0] }
        } else null

    companion object {
        const val METRIC_KEY = "step_count"
    }
}

@Singleton
class MotionDetectedMetricSource @Inject constructor(
    private val sensors: DeviceSensors,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Motion detected",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float =
        sensors.oneShot(Sensor.TYPE_MOTION_DETECT, timeoutMs = 500L) { if (it[0] > 0f) 1f else 0f } ?: 0f

    override fun stream(): Flow<Float>? =
        if (sensors.has(Sensor.TYPE_MOTION_DETECT)) {
            sensors.stream(Sensor.TYPE_MOTION_DETECT) { if (it[0] > 0f) 1f else 0f }
        } else null

    companion object {
        const val METRIC_KEY = "motion_detected"
    }
}
