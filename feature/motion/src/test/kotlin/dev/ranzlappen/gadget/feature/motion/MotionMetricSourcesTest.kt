package dev.ranzlappen.gadget.feature.motion

import android.hardware.Sensor
import dev.ranzlappen.gadget.core.hardware.DeviceSensors
import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the three [dev.ranzlappen.gadget.core.model.MetricSource]s
 * in `:feature:motion` (`RotationRateMetricSource`, `StepCounterMetricSource`,
 * `MotionDetectedMetricSource`) — all thin wrappers over [DeviceSensors], so
 * coverage focuses on the per-source `transform` lambda each one hands to
 * `DeviceSensors.oneShot`/`stream` (captured via `slot` and invoked directly
 * against synthetic sensor readings) and the sensor-absent short-circuit
 * (`stream()` returns null, `sample()` falls back to 0f).
 */
class MotionMetricSourcesTest {

    private val sensors = mockk<DeviceSensors>()

    // ---- RotationRateMetricSource ----

    @Test
    fun `rotation rate descriptor advertises rad-per-second in the Sensor category`() {
        val descriptor = RotationRateMetricSource(sensors).descriptor

        assertEquals(RotationRateMetricSource.METRIC_KEY, descriptor.metricKey)
        assertEquals("rotation_rate", descriptor.metricKey)
        assertEquals("rad/s", descriptor.unit)
        assertEquals(MetricCategory.Sensor, descriptor.category)
    }

    @Test
    fun `rotation rate transform computes the 3-axis vector magnitude`() {
        every { sensors.has(Sensor.TYPE_GYROSCOPE) } returns true
        val transformSlot = slot<(FloatArray) -> Float>()
        every { sensors.stream(Sensor.TYPE_GYROSCOPE, capture(transformSlot)) } returns emptyFlow()

        RotationRateMetricSource(sensors).stream()

        // 3-4-0 right triangle: magnitude is exactly 5.
        assertEquals(5f, transformSlot.captured(floatArrayOf(3f, 4f, 0f)))
    }

    @Test
    fun `rotation rate stream is null when the gyroscope is absent`() {
        every { sensors.has(Sensor.TYPE_GYROSCOPE) } returns false

        assertNull(RotationRateMetricSource(sensors).stream())
    }

    @Test
    fun `rotation rate sample falls back to zero when oneShot times out`() = runTest {
        coEvery { sensors.oneShot(Sensor.TYPE_GYROSCOPE, any(), any()) } returns null

        assertEquals(0f, RotationRateMetricSource(sensors).sample())
    }

    @Test
    fun `rotation rate sample returns oneShot's computed magnitude`() = runTest {
        val transformSlot = slot<(FloatArray) -> Float>()
        coEvery { sensors.oneShot(Sensor.TYPE_GYROSCOPE, any(), capture(transformSlot)) } answers {
            transformSlot.captured(floatArrayOf(0f, 0f, 2f))
        }

        assertEquals(2f, RotationRateMetricSource(sensors).sample())
    }

    // ---- StepCounterMetricSource ----

    @Test
    fun `step counter descriptor advertises an unbounded steps ceiling`() {
        val descriptor = StepCounterMetricSource(sensors).descriptor

        assertEquals("step_count", descriptor.metricKey)
        assertEquals("steps", descriptor.unit)
        assertEquals(Float.MAX_VALUE, descriptor.max)
    }

    @Test
    fun `step counter transform reports the raw first value untouched`() {
        every { sensors.has(Sensor.TYPE_STEP_COUNTER) } returns true
        val transformSlot = slot<(FloatArray) -> Float>()
        every { sensors.stream(Sensor.TYPE_STEP_COUNTER, capture(transformSlot)) } returns emptyFlow()

        StepCounterMetricSource(sensors).stream()

        assertEquals(4567f, transformSlot.captured(floatArrayOf(4567f)))
    }

    @Test
    fun `step counter stream is null when the sensor is absent`() {
        every { sensors.has(Sensor.TYPE_STEP_COUNTER) } returns false

        assertNull(StepCounterMetricSource(sensors).stream())
    }

    @Test
    fun `step counter sample falls back to zero when absent`() = runTest {
        coEvery { sensors.oneShot(Sensor.TYPE_STEP_COUNTER, any(), any()) } returns null

        assertEquals(0f, StepCounterMetricSource(sensors).sample())
    }

    // ---- MotionDetectedMetricSource ----

    @Test
    fun `motion detected descriptor is a 0-1 boolean-shaped Sensor metric`() {
        val descriptor = MotionDetectedMetricSource(sensors).descriptor

        assertEquals("motion_detected", descriptor.metricKey)
        assertEquals(0f, descriptor.min)
        assertEquals(1f, descriptor.max)
        assertEquals(MetricCategory.Sensor, descriptor.category)
    }

    @Test
    fun `motion detected transform maps a positive reading to 1`() {
        every { sensors.has(Sensor.TYPE_MOTION_DETECT) } returns true
        val transformSlot = slot<(FloatArray) -> Float>()
        every { sensors.stream(Sensor.TYPE_MOTION_DETECT, capture(transformSlot)) } returns emptyFlow()

        MotionDetectedMetricSource(sensors).stream()

        assertEquals(1f, transformSlot.captured(floatArrayOf(1f)))
    }

    @Test
    fun `motion detected transform maps a non-positive reading to 0`() {
        every { sensors.has(Sensor.TYPE_MOTION_DETECT) } returns true
        val transformSlot = slot<(FloatArray) -> Float>()
        every { sensors.stream(Sensor.TYPE_MOTION_DETECT, capture(transformSlot)) } returns emptyFlow()

        MotionDetectedMetricSource(sensors).stream()

        assertEquals(0f, transformSlot.captured(floatArrayOf(0f)))
    }

    @Test
    fun `motion detected stream is null when the sensor is absent`() {
        every { sensors.has(Sensor.TYPE_MOTION_DETECT) } returns false

        assertNull(MotionDetectedMetricSource(sensors).stream())
    }

    @Test
    fun `motion detected sample falls back to zero when absent`() = runTest {
        coEvery { sensors.oneShot(Sensor.TYPE_MOTION_DETECT, any(), any()) } returns null

        assertEquals(0f, MotionDetectedMetricSource(sensors).sample())
    }
}
