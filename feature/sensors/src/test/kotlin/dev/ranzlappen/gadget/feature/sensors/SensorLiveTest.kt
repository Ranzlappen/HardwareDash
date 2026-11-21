package dev.ranzlappen.gadget.feature.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorLiveTest {

    @Test
    fun `first event sets values and history but no rate`() {
        val s = SensorLive().next(floatArrayOf(1f, 2f), accuracy = 3, timestampNanos = 1_000L)
        assertEquals(listOf(1f, 2f), s.values)
        assertEquals(listOf(listOf(1f), listOf(2f)), s.history)
        assertEquals(3, s.accuracy)
        assertNull(s.rateHz)
    }

    @Test
    fun `rate follows event spacing`() {
        var s = SensorLive().next(floatArrayOf(0f), 3, 1_000_000_000L)
        s = s.next(floatArrayOf(0f), 3, 1_020_000_000L) // 20 ms → 50 Hz
        assertEquals(50f, s.rateHz!!, 0.01f)
        s = s.next(floatArrayOf(0f), 3, 1_030_000_000L) // 10 ms → 100 Hz, smoothed
        assertEquals(50f * 0.8f + 100f * 0.2f, s.rateHz!!, 0.01f)
    }

    @Test
    fun `history is capped per axis`() {
        var s = SensorLive()
        repeat(SensorLive.HISTORY_SIZE + 10) { i -> s = s.next(floatArrayOf(i.toFloat()), 3, (i + 1).toLong()) }
        assertEquals(SensorLive.HISTORY_SIZE, s.history[0].size)
        assertEquals((SensorLive.HISTORY_SIZE + 9).toFloat(), s.history[0].last())
    }

    @Test
    fun `a non-increasing timestamp keeps the previous rate`() {
        var s = SensorLive().next(floatArrayOf(0f), 3, 1_000_000_000L)
        s = s.next(floatArrayOf(0f), 3, 1_020_000_000L)
        val before = s.rateHz
        s = s.next(floatArrayOf(0f), 3, 1_020_000_000L)
        assertEquals(before, s.rateHz)
    }
}
