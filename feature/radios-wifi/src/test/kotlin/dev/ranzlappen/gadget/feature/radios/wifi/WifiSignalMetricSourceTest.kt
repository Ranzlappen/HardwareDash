package dev.ranzlappen.gadget.feature.radios.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
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
 * Unit tests for [WifiSignalMetricSource] — `:feature:radios-wifi`'s
 * monitoring `MetricSource` seam for RSSI. Covers the poll path
 * ([WifiSignalMetricSource.sample]) against a mocked [WifiManager]/[WifiInfo],
 * including the "no active connection" fallback to -100 dBm, and the push
 * path ([WifiSignalMetricSource.stream]) by capturing the [BroadcastReceiver]
 * passed to a mocked [Context.registerReceiver] and driving it manually with
 * `RSSI_CHANGED_ACTION` broadcasts, mirroring `BtEnabledMetricSourceTest`'s
 * "capture + drive the callback" technique.
 */
class WifiSignalMetricSourceTest {

    private fun contextWith(wifiManager: WifiManager): Context {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        return context
    }

    @Test
    fun `descriptor advertises the wifi_signal metric key as a Network metric with a dBm range`() {
        val wifiManager = mockk<WifiManager>()
        val source = WifiSignalMetricSource(contextWith(wifiManager))

        assertEquals(WifiSignalMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("wifi_signal", source.descriptor.metricKey)
        assertEquals(MetricCategory.Network, source.descriptor.category)
        assertEquals("dBm", source.descriptor.unit)
        assertEquals(-100f, source.descriptor.min)
        assertEquals(-30f, source.descriptor.max)
    }

    @Test
    fun `sample reports the connection info rssi when connected`() = runTest {
        val wifiManager = mockk<WifiManager>()
        val info = mockk<WifiInfo>()
        every { info.rssi } returns -55
        every { wifiManager.connectionInfo } returns info
        val source = WifiSignalMetricSource(contextWith(wifiManager))

        assertEquals(-55f, source.sample())
    }

    @Test
    fun `sample falls back to -100 dBm when there is no connection info`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.connectionInfo } returns null
        val source = WifiSignalMetricSource(contextWith(wifiManager))

        assertEquals(-100f, source.sample())
    }

    @Test
    fun `stream emits the current rssi on subscribe then follows RSSI_CHANGED broadcasts`() = runTest {
        val wifiManager = mockk<WifiManager>()
        val info = mockk<WifiInfo>()
        every { info.rssi } returns -70
        every { wifiManager.connectionInfo } returns info
        val context = contextWith(wifiManager)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = WifiSignalMetricSource(context)
        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()

        // Initial emission mirrors sample() at subscribe time.
        assertEquals(listOf(-70f), values)

        val rssiChanged = mockk<Intent>()
        every { rssiChanged.getIntExtra(WifiManager.EXTRA_NEW_RSSI, Int.MIN_VALUE) } returns -42
        receiverSlot.captured.onReceive(context, rssiChanged)
        advanceUntilIdle()

        assertEquals(listOf(-70f, -42f), values)

        job.cancel()
        advanceUntilIdle()
        verify { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun `stream ignores a broadcast with no rssi extra`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.connectionInfo } returns null
        val context = contextWith(wifiManager)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = WifiSignalMetricSource(context)
        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()
        assertEquals(listOf(-100f), values)

        val noExtra = mockk<Intent>()
        every { noExtra.getIntExtra(WifiManager.EXTRA_NEW_RSSI, Int.MIN_VALUE) } returns Int.MIN_VALUE
        receiverSlot.captured.onReceive(context, noExtra)
        advanceUntilIdle()

        // No second emission — the sentinel value is filtered out.
        assertEquals(listOf(-100f), values)
        job.cancel()
    }
}
