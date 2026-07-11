package dev.ranzlappen.gadget.feature.radios.wifi.automation

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.radios.wifi.WifiMonitor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [WifiActionHandler] — `:feature:radios-wifi`'s automation
 * `ActionHandler` seam. Mirrors `BtActionHandlerTest` / `AdbDebugActionHandlerTest`:
 * every branch only reads [WifiMonitor.state].
 *
 * [WifiMonitor] is a concrete (non-`open`) class, not an interface, so rather
 * than mocking it directly (which would pull in MockK's final-class inline
 * agent) each test builds a *real* [WifiMonitor] backed by a mocked
 * [Context]/[WifiManager] — the same technique used in `WifiMonitorTest` —
 * and drives it into the enabled/connected combination under test.
 */
class WifiActionHandlerTest {

    private fun monitorWith(enabled: Boolean, connected: Boolean): WifiMonitor {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns enabled
        if (enabled) {
            if (connected) {
                val info = mockk<WifiInfo>()
                every { info.networkId } returns 1
                every { info.ssid } returns WifiManager.UNKNOWN_SSID
                every { info.rssi } returns Int.MIN_VALUE
                every { info.linkSpeed } returns 0
                every { info.frequency } returns 0
                every { info.bssid } returns null
                every { wifiManager.connectionInfo } returns info
            } else {
                every { wifiManager.connectionInfo } returns null
            }
        }
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        return WifiMonitor(context)
    }

    private fun handlerWith(enabled: Boolean, connected: Boolean): WifiActionHandler =
        WifiActionHandler(monitorWith(enabled = enabled, connected = connected))

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        val handler = handlerWith(enabled = false, connected = false)

        assertEquals(WifiActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("wifi", handler.featureId)
    }

    @Test
    fun `declares exactly the check-enabled and check-connected actions`() {
        val handler = handlerWith(enabled = false, connected = false)

        assertEquals(2, handler.actions.size)
        val keys = handler.actions.map { it.key }
        assertEquals(
            listOf(WifiActionHandler.ACTION_CHECK_ENABLED, WifiActionHandler.ACTION_CHECK_CONNECTED),
            keys,
        )
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val handler = handlerWith(enabled = true, connected = true)

        val result = handler.dispatch("not-a-real-action", emptyMap())

        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `wifi_enabled_check succeeds when the monitor reports enabled`() = runTest {
        val handler = handlerWith(enabled = true, connected = false)

        val result = handler.dispatch(WifiActionHandler.ACTION_CHECK_ENABLED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `wifi_enabled_check fails with a reason when the monitor reports disabled`() = runTest {
        val handler = handlerWith(enabled = false, connected = false)

        val result = handler.dispatch(WifiActionHandler.ACTION_CHECK_ENABLED, emptyMap())

        assertEquals(ActionResult.Failure("WiFi is not enabled"), result)
    }

    @Test
    fun `wifi_connected_check succeeds when the monitor reports connected`() = runTest {
        val handler = handlerWith(enabled = true, connected = true)

        val result = handler.dispatch(WifiActionHandler.ACTION_CHECK_CONNECTED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `wifi_connected_check fails with a reason when the monitor reports disconnected`() = runTest {
        val handler = handlerWith(enabled = true, connected = false)

        val result = handler.dispatch(WifiActionHandler.ACTION_CHECK_CONNECTED, emptyMap())

        assertEquals(ActionResult.Failure("WiFi is not connected"), result)
    }

    @Test
    fun `wifi_connected_check fails when wifi is disabled outright`() = runTest {
        val handler = handlerWith(enabled = false, connected = false)

        val result = handler.dispatch(WifiActionHandler.ACTION_CHECK_CONNECTED, emptyMap())

        assertEquals(ActionResult.Failure("WiFi is not connected"), result)
    }

    @Test
    fun `wifi_enabled_check ignores unrecognised params`() = runTest {
        val handler = handlerWith(enabled = true, connected = false)

        val result = handler.dispatch(
            WifiActionHandler.ACTION_CHECK_ENABLED,
            mapOf("unused" to "value"),
        )

        assertEquals(ActionResult.Success, result)
    }
}
