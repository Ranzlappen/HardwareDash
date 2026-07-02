package dev.ranzlappen.gadget.feature.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryMetricSourceTest {

    @Test
    fun `half of ram used reports 50 percent`() {
        val pct = MemoryMetricSource.usedPercent(totalBytes = 8_000L, availBytes = 4_000L)
        assertEquals(50f, pct, 0.001f)
    }

    @Test
    fun `no ram available reports 100 percent`() {
        val pct = MemoryMetricSource.usedPercent(totalBytes = 8_000L, availBytes = 0L)
        assertEquals(100f, pct, 0.001f)
    }

    @Test
    fun `all ram available reports 0 percent`() {
        val pct = MemoryMetricSource.usedPercent(totalBytes = 8_000L, availBytes = 8_000L)
        assertEquals(0f, pct, 0.001f)
    }

    @Test
    fun `avail greater than total clamps to 0 percent`() {
        // A transient race can momentarily report avail > total; the reading
        // must never go negative.
        val pct = MemoryMetricSource.usedPercent(totalBytes = 8_000L, availBytes = 9_000L)
        assertEquals(0f, pct, 0.001f)
    }

    @Test
    fun `zero total ram reports 0 percent rather than dividing by zero`() {
        val pct = MemoryMetricSource.usedPercent(totalBytes = 0L, availBytes = 0L)
        assertEquals(0f, pct, 0.001f)
    }
}
