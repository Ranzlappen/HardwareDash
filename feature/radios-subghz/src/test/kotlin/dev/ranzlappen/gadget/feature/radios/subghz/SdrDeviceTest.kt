package dev.ranzlappen.gadget.feature.radios.subghz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the SDR id table and the derived [SubghzState] flags —
 * the only genuinely testable logic in the module (everything else reaches the
 * Android USB stack and lives behind instrumented tests).
 */
class SdrDeviceTest {

    @Test
    fun `match resolves a known vendor and product id`() {
        val device = SdrDevice.match(0x1d50, 0x605b)
        assertEquals(SdrDevice.YardStickOne, device)
    }

    @Test
    fun `match returns null for an unknown id pair`() {
        assertNull(SdrDevice.match(0x1234, 0x5678))
    }

    @Test
    fun `match is exact on both ids`() {
        // Right vendor, wrong product must not collide.
        assertNull(SdrDevice.match(0x0bda, 0x9999))
    }

    @Test
    fun `subghz capable devices are flagged`() {
        assertTrue(SdrDevice.YardStickOne.coversSubGhz)
        assertTrue(SdrDevice.HackRfOne.coversSubGhz)
        // A bare RTL2832U dongle starts above the sub-GHz ISM bands.
        assertFalse(SdrDevice.RtlSdr2832.coversSubGhz)
    }

    @Test
    fun `every id pair is unique`() {
        val pairs = SdrDevice.entries.map { it.vendorId to it.productId }
        assertEquals(pairs.size, pairs.toSet().size)
    }

    @Test
    fun `bridgeConnected mirrors device presence`() {
        assertFalse(SubghzState(usbHostAvailable = true, device = null).bridgeConnected)
        assertTrue(
            SubghzState(usbHostAvailable = true, device = SdrDevice.HackRfOne).bridgeConnected,
        )
    }
}
