package dev.ranzlappen.gadget.core.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorDownsamplingTest {

    @Test
    fun `caps the point count for a 24h window regardless of poll rate`() {
        val windowMs = 24L * 60 * 60 * 1_000 // 24h
        val bucketMs = MonitorDownsampling.bucketMs(windowMs, pollIntervalMs = 250L, maxPoints = 500L)
        val points = windowMs / bucketMs
        assertTrue("expected <= 500 points, got $points", points <= 500L)
    }

    @Test
    fun `never sub-samples below the poll interval`() {
        // A short window where the cap would allow a finer bucket than the
        // poll rate: bucket must clamp up to the poll interval.
        val windowMs = 60L * 1_000 // 1 min
        val bucketMs = MonitorDownsampling.bucketMs(windowMs, pollIntervalMs = 1_000L, maxPoints = 500L)
        assertEquals(1_000L, bucketMs)
    }

    @Test
    fun `never returns a bucket below 1ms`() {
        val bucketMs = MonitorDownsampling.bucketMs(windowMs = 0L, pollIntervalMs = 0L, maxPoints = 500L)
        assertTrue(bucketMs >= 1L)
    }

    @Test
    fun `widget cap yields fewer points than the in-app cap for the same window`() {
        val windowMs = 5L * 60 * 60 * 1_000 // 5h
        val inApp = windowMs / MonitorDownsampling.bucketMs(windowMs, 1_000L, MonitorDownsampling.IN_APP_MAX_POINTS)
        val widget = windowMs / MonitorDownsampling.bucketMs(windowMs, 1_000L, 120L)
        assertTrue(widget <= 120L)
        assertTrue(widget <= inApp)
    }
}
