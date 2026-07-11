package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Unit tests for [BtEnabledMetricSource] — `:feature:radios-bt`'s
 * monitoring `MetricSource` seam. Covers the poll path ([BtEnabledMetricSource.sample])
 * directly against a mocked [BluetoothAdapterWrapper], and the push path
 * ([BtEnabledMetricSource.stream]) by capturing the [BroadcastReceiver] passed to a mocked
 * [Context.registerReceiver] and driving it manually to simulate
 * `ACTION_STATE_CHANGED` broadcasts — the same "capture + drive the callback"
 * technique used for the GATT callback in `BtGattBatteryReaderTest`, since
 * this repo has no Robolectric shadow to deliver a real broadcast.
 */
class BtEnabledMetricSourceTest {

    private val adapter = mockk<BluetoothAdapterWrapper>()

    @Test
    fun `descriptor advertises the bt_enabled metric key as a Network metric`() {
        val source = BtEnabledMetricSource(mockk(relaxed = true), adapter)

        assertEquals(BtEnabledMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("bt_enabled", source.descriptor.metricKey)
        assertEquals(MetricCategory.Network, source.descriptor.category)
        assertEquals(0f, source.descriptor.min)
        assertEquals(1f, source.descriptor.max)
    }

    @Test
    fun `sample reports 1 when the adapter is enabled`() = runTest {
        every { adapter.isEnabled() } returns true
        val source = BtEnabledMetricSource(mockk(relaxed = true), adapter)

        assertEquals(1f, source.sample())
    }

    @Test
    fun `sample reports 0 when the adapter is disabled`() = runTest {
        every { adapter.isEnabled() } returns false
        val source = BtEnabledMetricSource(mockk(relaxed = true), adapter)

        assertEquals(0f, source.sample())
    }

    @Test
    fun `stream emits the current state on subscribe then follows STATE_ON broadcasts`() = runTest {
        every { adapter.isEnabled() } returns false
        val context = mockk<Context>(relaxed = true)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = BtEnabledMetricSource(context, adapter)
        val values = mutableListOf<Float>()
        val job = launch { source.stream()!!.toList(values) }
        advanceUntilIdle()

        // Initial emission mirrors sample() at subscribe time.
        assertEquals(listOf(0f), values)

        val stateOn = mockk<Intent>()
        every { stateOn.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) } returns
            BluetoothAdapter.STATE_ON
        receiverSlot.captured.onReceive(context, stateOn)
        advanceUntilIdle()

        assertEquals(listOf(0f, 1f), values)

        job.cancel()
        advanceUntilIdle()
        verify { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun `stream maps any non-STATE_ON broadcast to 0`() = runTest {
        every { adapter.isEnabled() } returns true
        val context = mockk<Context>(relaxed = true)
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any()) } returns null

        val source = BtEnabledMetricSource(context, adapter)
        val values = mutableListOf<Float>()
        val job = launch { source.stream()!!.toList(values) }
        advanceUntilIdle()
        assertEquals(listOf(1f), values)

        val stateOff = mockk<Intent>()
        every { stateOff.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) } returns
            BluetoothAdapter.STATE_OFF
        receiverSlot.captured.onReceive(context, stateOff)
        advanceUntilIdle()

        assertEquals(listOf(1f, 0f), values)
        job.cancel()
    }
}
