package dev.ranzlappen.gadget.feature.radios.nfc

import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [NfcEnabledMetricSource] — `:feature:radios-nfc`'s
 * monitoring `MetricSource` seam. Unlike `BtEnabledMetricSource` (which
 * pushes off a `BroadcastReceiver`), this source's [NfcEnabledMetricSource.stream]
 * is itself a poll loop (`emit(sample()); delay(...)`), so it's driven purely
 * through `runTest`'s virtual-time auto-advance — no `Context`/receiver
 * capture needed.
 */
class NfcEnabledMetricSourceTest {

    private val adapter = mockk<NfcAdapterWrapper>()

    @Test
    fun `descriptor advertises the nfc_enabled metric key as a Network metric`() {
        val source = NfcEnabledMetricSource(adapter)

        assertEquals(NfcEnabledMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("nfc_enabled", source.descriptor.metricKey)
        assertEquals(MetricCategory.Network, source.descriptor.category)
        assertEquals(0f, source.descriptor.min)
        assertEquals(1f, source.descriptor.max)
    }

    @Test
    fun `sample reports 1 when the adapter is enabled`() = runTest {
        every { adapter.isEnabled() } returns true
        val source = NfcEnabledMetricSource(adapter)

        assertEquals(1f, source.sample())
    }

    @Test
    fun `sample reports 0 when the adapter is disabled`() = runTest {
        every { adapter.isEnabled() } returns false
        val source = NfcEnabledMetricSource(adapter)

        assertEquals(0f, source.sample())
    }

    @Test
    fun `stream polls sample on every interval`() = runTest {
        every { adapter.isEnabled() } returnsMany listOf(true, false, true)
        val source = NfcEnabledMetricSource(adapter)

        val values = source.stream().take(3).toList()

        assertEquals(listOf(1f, 0f, 1f), values)
    }

    @Test
    fun `stream is a non-null poll source`() {
        every { adapter.isEnabled() } returns true
        val source = NfcEnabledMetricSource(adapter)

        assertEquals(true, source.stream() != null)
    }
}
