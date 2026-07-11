package dev.ranzlappen.gadget.feature.ambient

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [AmbientSensor] — the shared light-sensor state holder that
 * both [AmbientViewModel] and [dev.ranzlappen.gadget.feature.ambient.automation.AmbientActionHandler]
 * read from. Covers the constructor's sensor-presence gate (registers only
 * when a light sensor exists) and the listener's `onSensorChanged` state
 * update, including the (currently dead in production, but real) branch that
 * ignores an event whose sensor type isn't `TYPE_LIGHT`.
 *
 * [SensorEvent] has no public constructor and a final `values` field, so
 * [fakeSensorEvent] builds one via `mockk` (bypasses the constructor) plus
 * reflection on the `sensor`/`values` fields — see
 * [AmbientLightMetricSourceTest] for the same technique on the sibling
 * `MetricSource`.
 */
class AmbientSensorTest {

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
    fun `state reports sensorAvailable true and a null lux level when a light sensor exists`() {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor

        val ambientSensor = AmbientSensor(contextWith(sensorManager))

        assertEquals(true, ambientSensor.state.value.sensorAvailable)
        assertNull(ambientSensor.state.value.luxLevel)
    }

    @Test
    fun `state reports sensorAvailable false and never registers when there is no light sensor`() {
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns null

        val ambientSensor = AmbientSensor(contextWith(sensorManager))

        assertEquals(false, ambientSensor.state.value.sensorAvailable)
        verify(exactly = 0) {
            sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>())
        }
    }

    @Test
    fun `registers the listener against the light sensor at normal delay when present`() {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor

        AmbientSensor(contextWith(sensorManager))

        verify { sensorManager.registerListener(any(), sensor, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    @Test
    fun `a TYPE_LIGHT sensor event updates the lux level`() {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val listenerSlot = slot<SensorEventListener>()
        every {
            sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } returns true

        val ambientSensor = AmbientSensor(contextWith(sensorManager))
        listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_LIGHT, 450f))

        assertEquals(450f, ambientSensor.state.value.luxLevel)
    }

    @Test
    fun `a non-light sensor event is ignored`() {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val listenerSlot = slot<SensorEventListener>()
        every {
            sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } returns true

        val ambientSensor = AmbientSensor(contextWith(sensorManager))
        listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_PROXIMITY, 8f))

        assertNull(ambientSensor.state.value.luxLevel)
    }

    @Test
    fun `repeated TYPE_LIGHT events keep updating the latest lux level`() {
        val sensor = mockk<Sensor>()
        val sensorManager = mockk<SensorManager>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns sensor
        val listenerSlot = slot<SensorEventListener>()
        every {
            sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } returns true

        val ambientSensor = AmbientSensor(contextWith(sensorManager))
        listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_LIGHT, 10f))
        listenerSlot.captured.onSensorChanged(fakeSensorEvent(Sensor.TYPE_LIGHT, 999f))

        assertEquals(999f, ambientSensor.state.value.luxLevel)
    }
}
