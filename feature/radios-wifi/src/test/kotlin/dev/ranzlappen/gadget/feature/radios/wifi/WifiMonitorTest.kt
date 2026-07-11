package dev.ranzlappen.gadget.feature.radios.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [WifiMonitor]'s `buildCurrentState()` state-transition
 * logic — the one piece of real business logic in this module beyond plain
 * passthrough: the enabled/connected gate, SSID quote-stripping and
 * `UNKNOWN_SSID` filtering, the `rssi`/`linkSpeed`/`frequency` sentinel-value
 * nulling (`takeIf { it > ... }`), the randomized-MAC BSSID mask
 * (`02:00:00:00:00:00`), and the broadcast-driven state rebuild.
 *
 * No Robolectric shadow is available, so — like `WifiEnabledMetricSourceTest`
 * — [Context.registerReceiver] is mocked and the captured [BroadcastReceiver]
 * is driven manually to simulate a real broadcast.
 */
class WifiMonitorTest {

    private fun contextWith(wifiManager: WifiManager): Pair<Context, CapturingSlot<BroadcastReceiver>> {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null
        return context to receiverSlot
    }

    private fun disconnectedInfo(): WifiInfo = mockk<WifiInfo>().also {
        every { it.networkId } returns -1
    }

    private fun connectedInfo(
        ssid: String = "\"Home\"",
        rssi: Int = -50,
        linkSpeed: Int = 150,
        frequency: Int = 5180,
        bssid: String? = "AA:BB:CC:DD:EE:FF",
    ): WifiInfo = mockk<WifiInfo>().also {
        every { it.networkId } returns 1
        every { it.ssid } returns ssid
        every { it.rssi } returns rssi
        every { it.linkSpeed } returns linkSpeed
        every { it.frequency } returns frequency
        every { it.bssid } returns bssid
    }

    @Test
    fun `disabled wifi produces the all-default disabled state`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns false
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertEquals(WifiState(enabled = false), state)
    }

    @Test
    fun `enabled wifi with no connection info is enabled but not connected`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns null
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertTrue(state.enabled)
        assertFalse(state.connected)
        assertNull(state.ssid)
        assertNull(state.rssiDbm)
        assertNull(state.bssid)
    }

    @Test
    fun `enabled wifi with networkId -1 is enabled but not connected`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns disconnectedInfo()
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertTrue(state.enabled)
        assertFalse(state.connected)
        assertNull(state.ssid)
    }

    @Test
    fun `connected state strips surrounding quotes from the ssid`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(ssid = "\"MyNetwork\"")
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertTrue(state.connected)
        assertEquals("MyNetwork", state.ssid)
    }

    @Test
    fun `connected state maps UNKNOWN_SSID to null`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(ssid = WifiManager.UNKNOWN_SSID)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.ssid)
    }

    @Test
    fun `connected state leaves an unquoted ssid untouched`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(ssid = "0xA1B2C3")
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertEquals("0xA1B2C3", state.ssid)
    }

    @Test
    fun `connected state nulls rssi at the MIN_VALUE sentinel`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(rssi = Int.MIN_VALUE)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.rssiDbm)
    }

    @Test
    fun `connected state preserves a real rssi reading`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(rssi = -63)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertEquals(-63, state.rssiDbm)
    }

    @Test
    fun `connected state nulls a non-positive link speed`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(linkSpeed = 0)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.linkSpeedMbps)
    }

    @Test
    fun `connected state preserves a positive link speed`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(linkSpeed = 433)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertEquals(433, state.linkSpeedMbps)
    }

    @Test
    fun `connected state nulls a non-positive frequency`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(frequency = 0)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.frequencyMhz)
    }

    @Test
    fun `connected state masks the randomized-MAC placeholder bssid`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(bssid = "02:00:00:00:00:00")
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.bssid)
    }

    @Test
    fun `connected state preserves a real bssid`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(bssid = "AA:BB:CC:DD:EE:FF")
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertEquals("AA:BB:CC:DD:EE:FF", state.bssid)
    }

    @Test
    fun `connected state nulls a null bssid`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(bssid = null)
        val (context, _) = contextWith(wifiManager)

        val state = WifiMonitor(context).state.value

        assertNull(state.bssid)
    }

    @Test
    fun `a broadcast rebuilds state from the latest wifi manager reading`() {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns false
        val (context, receiverSlot) = contextWith(wifiManager)

        val monitor = WifiMonitor(context)
        assertFalse(monitor.state.value.enabled)

        // Simulate wifi being turned on and connecting, then fire the broadcast
        // the monitor is subscribed to.
        every { wifiManager.isWifiEnabled } returns true
        every { wifiManager.connectionInfo } returns connectedInfo(ssid = "\"NewNetwork\"")

        val intent = mockk<Intent>()
        receiverSlot.captured.onReceive(context, intent)

        val state = monitor.state.value
        assertTrue(state.enabled)
        assertTrue(state.connected)
        assertEquals("NewNetwork", state.ssid)
    }
}
