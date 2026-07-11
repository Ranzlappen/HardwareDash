package dev.ranzlappen.gadget.feature.ambient

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AmbientLightMetricSource] — `:feature:ambient`'s monitoring
 * `MetricSource` seam for the light sensor. [SensorEvent] can't be
 * constructed through its public API (package-private constructor, final
 * `values` field), so [fakeSensorEvent] allocates one via `mockk` (which
 * bypasses the constructor) and reflectively pokes the `sensor`/`values`
 * fields — the sensor analogue of the "capture + drive the callback"
 * technique [WifiEnabledMetricSourceTest]/[LockStateMetricSourceTest] use for
 * a captured `BroadcastReceiver`.
 *
 * [sample] previously registered a [SensorEventListener] and unregistered it
 * on the very next line without ever suspending for a callback, so it always
 * returned the initial `0f` regardless of the real reading — a real bug,
 * since `RuleFireExecutor`/`AutomationService` call `sample()` directly
 * (bypassing `stream()`) to evaluate automation conditions on this metric.
 * Fixed alongside these tests; see the production-code comment.
 */
class AmbientLightMetricSourceTest {

    private fun contextWith(sensorManager: SensorManager): Context {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        return context
    }

    private fun fakeSensorEvent(type: Int, value: Float): SensorEvent {
        val sensor = mockk<Sensor>()
        every { sensor.type } returns type
        val event = mockk<SensorEvent>()
        setField(event, "sensor", sensor)
        setField(event, "values", floatArrayOf(value))
        return event
    }

    private fun setField(target: Any, name: String, value: Any) {
        val field = SensorEvent::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    @Test
    fun `descriptor advertises the ambient_light metric key as a Sensor metric`() {
        val sensorManager = mockk<SensorManager>(relaxed = true)
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        assertEquals(AmbientLightMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("ambient_light", source.descriptor.metricKey)
        assertEquals("lux", source.descriptor.unit)
        assertEquals(MetricCategory.Sensor, source.descriptor.category)
        assertEquals(0f, source.descriptor.min)
        assertEquals(20_000f, source.descriptor.max)
    }

    @Test
    fun `sample reports 0 when there is no light sensor`() = runTest {
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns null
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        assertEquals(0f, source.sample())
        verify(exactly = 0) {
            sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>())
        }
    }

    @Test
    fun `sample returns the sensor's first reported reading`() = runTest {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val listenerSlot = slot<SensorEventListener>()
        every {
            sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } answers {
            // Simulate an on-change sensor reporting immediately on registration.
            listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_LIGHT, 321f))
            true
        }
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        assertEquals(321f, source.sample())
        verify { sensorManager.unregisterListener(listenerSlot.captured) }
    }

    @Test
    fun `sample falls back to 0 and unregisters when the sensor never responds`() = runTest {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        assertEquals(0f, source.sample())
        verify { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    @Test
    fun `stream emits a synthetic 0 then closes without registering when there is no sensor`() = runTest {
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns null
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        val values = source.stream().toList()

        assertEquals(listOf(0f), values)
        verify(exactly = 0) {
            sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>())
        }
    }

    @Test
    fun `stream emits a synthetic 0 on subscribe then forwards real sensor readings`() = runTest {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val listenerSlot = slot<SensorEventListener>()
        every {
            sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } returns true
        val source = AmbientLightMetricSource(contextWith(sensorManager))

        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()

        assertEquals(listOf(0f), values)

        listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_LIGHT, 777f))
        advanceUntilIdle()

        assertEquals(listOf(0f, 777f), values)

        job.cancel()
        advanceUntilIdle()
        verify { sensorManager.unregisterListener(listenerSlot.captured) }
    }
}
