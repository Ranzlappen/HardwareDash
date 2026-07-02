package dev.ranzlappen.gadget.feature.radios.wifi.rooted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRootCommandsTest {

    @Test
    fun `rfkill builds block and unblock commands`() {
        assertEquals("rfkill block wifi", WifiRootCommands.rfkill(blocked = true))
        assertEquals("rfkill unblock wifi", WifiRootCommands.rfkill(blocked = false))
    }

    @Test
    fun `tx power clamps to the 20 dBm ceiling and converts to mBm`() {
        // 18 dBm → 1800 mBm, within bounds.
        assertEquals("iw phy phy0 set txpower fixed 1800", WifiRootCommands.setTxPower(18))
        // 50 dBm is clamped to 20 dBm → 2000 mBm.
        assertEquals("iw phy phy0 set txpower fixed 2000", WifiRootCommands.setTxPower(50))
    }

    @Test
    fun `tx power clamps a negative request up to the floor`() {
        assertEquals(WifiRootCommands.MIN_TX_POWER_DBM, WifiRootCommands.clampTxPowerDbm(-5))
        assertEquals("iw phy phy0 set txpower fixed 0", WifiRootCommands.setTxPower(-5))
    }

    @Test
    fun `channel allow-list accepts 2_4GHz and 5GHz entries, rejects others`() {
        assertTrue(WifiRootCommands.isAllowedChannel(6))
        assertTrue(WifiRootCommands.isAllowedChannel(36))
        assertTrue(WifiRootCommands.isAllowedChannel(165))
        assertFalse(WifiRootCommands.isAllowedChannel(0))
        assertFalse(WifiRootCommands.isAllowedChannel(15))
        assertFalse(WifiRootCommands.isAllowedChannel(200))
    }

    @Test
    fun `set channel builds the iw command`() {
        assertEquals("iw dev wlan0 set channel 44", WifiRootCommands.setChannel(44))
    }

    @Test
    fun `phy-info parsing detects monitor and IBSS support`() {
        val phyInfo = """
            Supported interface modes:
                 * managed
                 * monitor
                 * IBSS
        """.trimIndent()
        assertTrue(WifiRootCommands.supportsMonitor(phyInfo))
        assertTrue(WifiRootCommands.supportsIbss(phyInfo))

        val managedOnly = "Supported interface modes:\n\t * managed"
        assertFalse(WifiRootCommands.supportsMonitor(managedOnly))
        assertFalse(WifiRootCommands.supportsIbss(managedOnly))
    }
}
