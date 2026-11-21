package dev.ranzlappen.gadget.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStatusTest {

    private fun status(
        values: Map<String, Float?>,
        torch: Boolean = true,
        buzz: Boolean = true,
        log: LastLogEntry? = null,
    ) = homeStatusOf(values, torch, buzz, log)

    @Test
    fun `no readings yet means every card is unknown`() {
        val s = status(emptyMap(), torch = false, buzz = false)
        assertNull(s.batteryPercent)
        assertNull(s.storageUsedPercent)
        assertNull(s.wifiEnabled)
        assertNull(s.torchOn)
        assertFalse(s.canBuzz)
    }

    @Test
    fun `battery and storage are rounded and clamped`() {
        val s = status(
            mapOf(
                HomeKeys.BATTERY_LEVEL to 86.6f,
                HomeKeys.BATTERY_TEMPERATURE to 31.4f,
                HomeKeys.STORAGE_USED_PERCENT to 104f,
                HomeKeys.STORAGE_FREE_GB to 12.25f,
            ),
        )
        assertEquals(87, s.batteryPercent)
        assertEquals(31.4f, s.batteryTemperatureC)
        assertEquals(100, s.storageUsedPercent)
        assertEquals(12.25f, s.storageFreeGb)
    }

    @Test
    fun `zero battery temperature is treated as unknown`() {
        assertNull(status(mapOf(HomeKeys.BATTERY_TEMPERATURE to 0f)).batteryTemperatureC)
    }

    @Test
    fun `wifi connected shows rssi, disconnected and off do not`() {
        val connected = status(mapOf(HomeKeys.WIFI_ENABLED to 1f, HomeKeys.WIFI_SIGNAL to -57.6f))
        assertEquals(true, connected.wifiEnabled)
        assertEquals(-58, connected.wifiRssiDbm)

        val disconnected = status(mapOf(HomeKeys.WIFI_ENABLED to 1f, HomeKeys.WIFI_SIGNAL to -127f))
        assertEquals(true, disconnected.wifiEnabled)
        assertNull(disconnected.wifiRssiDbm)

        val off = status(mapOf(HomeKeys.WIFI_ENABLED to 0f, HomeKeys.WIFI_SIGNAL to -50f))
        assertEquals(false, off.wifiEnabled)
        assertNull(off.wifiRssiDbm)
    }

    @Test
    fun `torch state needs both the metric and the action`() {
        assertEquals(true, status(mapOf(HomeKeys.TORCH_INTENSITY to 100f)).torchOn)
        assertEquals(false, status(mapOf(HomeKeys.TORCH_INTENSITY to 0f)).torchOn)
        assertNull(status(mapOf(HomeKeys.TORCH_INTENSITY to 100f), torch = false).torchOn)
        assertNull(status(emptyMap()).torchOn)
    }

    @Test
    fun `last log entry and buzz availability pass through`() {
        val entry = LastLogEntry("note", 1_000L)
        val s = status(emptyMap(), buzz = true, log = entry)
        assertTrue(s.canBuzz)
        assertEquals(entry, s.lastLog)
    }

    @Test
    fun `every observed key has a positive poll interval`() {
        assertTrue(HomeKeys.OBSERVED.values.all { it > 0 })
        assertTrue(HomeKeys.TORCH_INTENSITY in HomeKeys.OBSERVED)
    }
}
