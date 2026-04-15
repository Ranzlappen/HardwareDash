package com.gadget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMetricTest {

    // ── fromKey ─────────────────────────────────────────────────────────────

    @Test
    fun `fromKey returns BATTERY_LEVEL for battery_level`() {
        val metric = WidgetMetric.fromKey("battery_level")
        assertNotNull(metric)
        assertEquals(WidgetMetric.BATTERY_LEVEL, metric)
        assertEquals("Battery Level", metric!!.displayName)
        assertEquals("Battery", metric.category)
        assertEquals("%", metric.unit)
    }

    @Test
    fun `fromKey returns WIFI_SIGNAL for wifi_signal`() {
        val metric = WidgetMetric.fromKey("wifi_signal")
        assertNotNull(metric)
        assertEquals(WidgetMetric.WIFI_SIGNAL, metric)
        assertEquals("WiFi Signal", metric!!.displayName)
        assertEquals("Network", metric.category)
        assertEquals("dBm", metric.unit)
    }

    @Test
    fun `fromKey returns correct metric for all valid keys`() {
        val expectedKeys = listOf(
            "battery_level", "battery_status", "battery_temp", "battery_voltage",
            "battery_health", "battery_current", "battery_charge_time",
            "wifi_ssid", "wifi_signal", "wifi_speed", "wifi_freq",
            "bt_status", "cell_signal", "net_type", "nfc_status",
            "accel", "gyro", "magneto", "light", "proximity",
            "barometer", "ambient_temp", "humidity", "steps",
            "brightness",
            "gps_location", "gps_altitude", "gps_speed", "gps_lat", "gps_lon",
        )
        for (key in expectedKeys) {
            val metric = WidgetMetric.fromKey(key)
            assertNotNull("Expected non-null metric for key '$key'", metric)
            assertEquals("Key mismatch for '$key'", key, metric!!.key)
        }
    }

    @Test
    fun `fromKey returns null for unknown key`() {
        assertNull(WidgetMetric.fromKey("nonexistent_metric"))
    }

    @Test
    fun `fromKey returns null for empty key`() {
        assertNull(WidgetMetric.fromKey(""))
    }

    @Test
    fun `fromKey is case sensitive`() {
        assertNull(WidgetMetric.fromKey("BATTERY_LEVEL"))
        assertNull(WidgetMetric.fromKey("Battery_Level"))
    }

    // ── grouped ─────────────────────────────────────────────────────────────

    @Test
    fun `grouped returns non-empty map`() {
        val groups = WidgetMetric.grouped()
        assertTrue(groups.isNotEmpty())
    }

    @Test
    fun `grouped contains Battery category`() {
        val groups = WidgetMetric.grouped()
        assertTrue("Expected 'Battery' category", groups.containsKey("Battery"))
        val batteryMetrics = groups["Battery"]!!
        assertTrue("Expected at least one Battery metric", batteryMetrics.isNotEmpty())
        assertTrue(
            "Battery category should contain BATTERY_LEVEL",
            batteryMetrics.contains(WidgetMetric.BATTERY_LEVEL),
        )
    }

    @Test
    fun `grouped contains Network category with WiFi metrics`() {
        val groups = WidgetMetric.grouped()
        assertTrue("Expected 'Network' category", groups.containsKey("Network"))
        val networkMetrics = groups["Network"]!!
        assertTrue(
            "Network category should contain WIFI_SIGNAL",
            networkMetrics.contains(WidgetMetric.WIFI_SIGNAL),
        )
    }

    @Test
    fun `grouped contains Sensors category`() {
        val groups = WidgetMetric.grouped()
        assertTrue("Expected 'Sensors' category", groups.containsKey("Sensors"))
        val sensorMetrics = groups["Sensors"]!!
        assertTrue(
            "Sensors category should contain ACCELEROMETER",
            sensorMetrics.contains(WidgetMetric.ACCELEROMETER),
        )
    }

    @Test
    fun `grouped contains Device category`() {
        val groups = WidgetMetric.grouped()
        assertTrue("Expected 'Device' category", groups.containsKey("Device"))
    }

    @Test
    fun `grouped contains Location category`() {
        val groups = WidgetMetric.grouped()
        assertTrue("Expected 'Location' category", groups.containsKey("Location"))
        val locationMetrics = groups["Location"]!!
        assertTrue(
            "Location category should contain GPS_LOCATION",
            locationMetrics.contains(WidgetMetric.GPS_LOCATION),
        )
    }

    @Test
    fun `grouped covers all metrics`() {
        val groups = WidgetMetric.grouped()
        val totalGrouped = groups.values.sumOf { it.size }
        assertEquals(
            "All enum entries should be present in grouped()",
            WidgetMetric.entries.size,
            totalGrouped,
        )
    }

    @Test
    fun `every metric has a non-blank displayName and key`() {
        for (metric in WidgetMetric.entries) {
            assertTrue(
                "Metric ${metric.name} has blank key",
                metric.key.isNotBlank(),
            )
            assertTrue(
                "Metric ${metric.name} has blank displayName",
                metric.displayName.isNotBlank(),
            )
        }
    }
}
