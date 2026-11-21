package dev.ranzlappen.gadget.feature.sensors

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorCatalogTest {

    @Test
    fun `type labels come from the string type, vendor types included`() {
        assertEquals("Accelerometer", sensorTypeLabel("android.sensor.accelerometer", Sensor.TYPE_ACCELEROMETER))
        assertEquals(
            "Magnetic field uncalibrated",
            sensorTypeLabel("android.sensor.magnetic_field_uncalibrated", Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
        )
        assertEquals("Light cct", sensorTypeLabel("com.vendor.sensor.light_cct", 65_587))
        assertEquals("Type 65587", sensorTypeLabel(null, 65_587))
        assertEquals("Type 7", sensorTypeLabel("", 7))
    }

    @Test
    fun `standard types know their unit, axes and group`() {
        val accel = sensorKind(Sensor.TYPE_ACCELEROMETER)
        assertEquals("m/s²", accel.unit)
        assertEquals(listOf("X", "Y", "Z"), accel.axes)
        assertEquals(SensorGroup.Motion, accel.group)

        assertEquals(SensorGroup.Position, sensorKind(Sensor.TYPE_MAGNETIC_FIELD).group)
        assertEquals("µT", sensorKind(Sensor.TYPE_MAGNETIC_FIELD).unit)
        assertEquals(SensorGroup.Environment, sensorKind(Sensor.TYPE_PRESSURE).group)
        assertEquals("hPa", sensorKind(Sensor.TYPE_PRESSURE).unit)
        assertEquals(6, sensorKind(Sensor.TYPE_GYROSCOPE_UNCALIBRATED).axes.size)
    }

    @Test
    fun `unknown types fall back to indexed axis names in Other`() {
        val kind = sensorKind(65_587)
        assertEquals(SensorGroup.Other, kind.group)
        assertEquals("", kind.unit)
        assertEquals("Value 1", axisLabel(kind, 0))
        assertEquals("Value 3", axisLabel(kind, 2))
        assertEquals("Z", axisLabel(sensorKind(Sensor.TYPE_ACCELEROMETER), 2))
        assertEquals("Value 4", axisLabel(sensorKind(Sensor.TYPE_ACCELEROMETER), 3))
    }

    @Test
    fun `persisted metric keys stay stable`() {
        assertEquals("proximity", metricKeyFor(Sensor.TYPE_PROXIMITY))
        assertEquals("light", metricKeyFor(Sensor.TYPE_LIGHT))
        assertEquals("acceleration", metricKeyFor(Sensor.TYPE_ACCELEROMETER))
        assertEquals("rotation_rate", metricKeyFor(Sensor.TYPE_GYROSCOPE))
        assertEquals("step_count", metricKeyFor(Sensor.TYPE_STEP_COUNTER))
        assertEquals("motion_detected", metricKeyFor(Sensor.TYPE_MOTION_DETECT))
        assertNull(metricKeyFor(Sensor.TYPE_PRESSURE))
        assertEquals("ambient_light", AmbientLightMetricSource.METRIC_KEY)
    }

    @Test
    fun `only step sensors need activity recognition`() {
        assertTrue(needsActivityRecognition(Sensor.TYPE_STEP_COUNTER))
        assertTrue(needsActivityRecognition(Sensor.TYPE_STEP_DETECTOR))
        assertFalse(needsActivityRecognition(Sensor.TYPE_ACCELEROMETER))
    }

    @Test
    fun `report lists specs, flags and labelled values`() {
        val info = previewSensor(0, Sensor.TYPE_ACCELEROMETER, "Accelerometer", SensorGroup.Motion, "m/s²", listOf("X", "Y", "Z"))
            .copy(isWakeUp = true)
        val report = sensorReport(info, floatArrayOf(0f, 9.81f, -1.5f))
        assertTrue(report, report.startsWith("Accelerometer — LSM6DSO Accelerometer"))
        assertTrue(report, report.contains("Vendor: STMicro"))
        assertTrue(report, report.contains("Flags: wake-up"))
        assertTrue(report, report.contains("Y: 9.810 m/s²"))
        assertTrue(report, report.contains("Z: -1.500 m/s²"))
        assertFalse(report, report.endsWith("\n"))
    }

    @Test
    fun `one-shot reporting mode marks trigger sensors`() {
        val base = previewSensor(0, Sensor.TYPE_SIGNIFICANT_MOTION, "Significant motion", SensorGroup.Motion, "", emptyList())
        assertFalse(base.isOneShot)
        assertTrue(base.copy(reportingMode = Sensor.REPORTING_MODE_ONE_SHOT).isOneShot)
    }
}
