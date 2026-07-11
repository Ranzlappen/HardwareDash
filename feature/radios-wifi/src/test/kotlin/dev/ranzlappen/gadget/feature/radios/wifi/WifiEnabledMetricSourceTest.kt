package dev.ranzlappen.gadget.feature.radios.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Unit tests for [WifiEnabledMetricSource] — `:feature:radios-wifi`'s
 * monitoring `MetricSource` seam for the on/off signal. Covers the poll path
 * ([WifiEnabledMetricSource.sample]) against a mocked [WifiManager], and the
 * push path ([WifiEnabledMetricSource.stream]) by capturing the
 * [BroadcastReceiver] passed to a mocked [Context.registerReceiver] and
 * driving it manually to simulate `WIFI_STATE_CHANGED_ACTION` broadcasts —
 * the same "capture + drive the callback" technique used in
 * `BtEnabledMetricSourceTest`, since this repo has no Robolectric shadow to
 * deliver a real broadcast.
 *
 * Unlike the Bluetooth sibling (which injects a wrapper interface),
 * [WifiEnabledMetricSource] pulls its [WifiManager] straight from
 * `context.applicationContext.getSystemService(...)` in the constructor, so
 * every test wires a [Context] mock that resolves to a mocked [WifiManager].
 */
class WifiEnabledMetricSourceTest {

    private fun contextWith(wifiManager: WifiManager): Context {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        return context
    }

    @Test
    fun `descriptor advertises the wifi_enabled metric key as a Network metric`() {
        val wifiManager = mockk<WifiManager>()
        val source = WifiEnabledMetricSource(contextWith(wifiManager))

        assertEquals(WifiEnabledMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("wifi_enabled", source.descriptor.metricKey)
        assertEquals(MetricCategory.Network, source.descriptor.category)
        assertEquals(0f, source.descriptor.min)
        assertEquals(1f, source.descriptor.max)
    }

    @Test
    fun `sample reports 1 when wifi is enabled`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        val source = WifiEnabledMetricSource(contextWith(wifiManager))

        assertEquals(1f, source.sample())
    }

    @Test
    fun `sample reports 0 when wifi is disabled`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns false
        val source = WifiEnabledMetricSource(contextWith(wifiManager))

        assertEquals(0f, source.sample())
    }

    @Test
    fun `stream emits the current state on subscribe then follows WIFI_STATE_ENABLED broadcasts`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns false
        val context = contextWith(wifiManager)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = WifiEnabledMetricSource(context)
        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()

        // Initial emission mirrors sample() at subscribe time.
        assertEquals(listOf(0f), values)

        val stateEnabled = mockk<Intent>()
        every {
            stateEnabled.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        } returns WifiManager.WIFI_STATE_ENABLED
        receiverSlot.captured.onReceive(context, stateEnabled)
        advanceUntilIdle()

        assertEquals(listOf(0f, 1f), values)

        job.cancel()
        advanceUntilIdle()
        verify { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun `stream maps any non-ENABLED broadcast to 0`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        val context = contextWith(wifiManager)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = WifiEnabledMetricSource(context)
        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()
        assertEquals(listOf(1f), values)

        val stateDisabling = mockk<Intent>()
        every {
            stateDisabling.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        } returns WifiManager.WIFI_STATE_DISABLING
        receiverSlot.captured.onReceive(context, stateDisabling)
        advanceUntilIdle()

        assertEquals(listOf(1f, 0f), values)
        job.cancel()
    }

    @Test
    fun `stream treats a missing wifi-state extra as disabled`() = runTest {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns true
        val context = contextWith(wifiManager)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = WifiEnabledMetricSource(context)
        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()

        val noExtra = mockk<Intent>()
        every {
            noExtra.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        } returns WifiManager.WIFI_STATE_UNKNOWN
        receiverSlot.captured.onReceive(context, noExtra)
        advanceUntilIdle()

        assertEquals(listOf(1f, 0f), values)
        job.cancel()
    }
}
