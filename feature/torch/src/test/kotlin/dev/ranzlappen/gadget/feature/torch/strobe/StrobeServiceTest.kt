package dev.ranzlappen.gadget.feature.torch.strobe

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-logic tests for [StrobeService.Companion.halfPeriodMillis].
 *
 * The companion function maps a strobe rate (Hz) to the per-flip
 * `delay(...)` value used by the foreground service's loop. Verified
 * here in isolation because the rest of the service depends on the
 * Android framework + Camera2 and isn't JVM-testable without
 * Robolectric (deferred to the instrumented test pass — issue #92).
 */
class StrobeServiceTest {

    @Test
    fun `5 Hz yields 100ms half period`() {
        assertEquals(100L, StrobeService.halfPeriodMillis(5f))
    }

    @Test
    fun `1 Hz yields 500ms half period`() {
        assertEquals(500L, StrobeService.halfPeriodMillis(1f))
    }

    @Test
    fun `20 Hz yields 25ms half period`() {
        assertEquals(25L, StrobeService.halfPeriodMillis(20f))
    }

    @Test
    fun `rate above MAX clamps to MAX`() {
        // 100 Hz would mathematically map to 5 ms; the clamp at
        // MAX_RATE_HZ (20) and the absolute 25 ms floor combine to
        // 25 ms.
        assertEquals(25L, StrobeService.halfPeriodMillis(100f))
    }

    @Test
    fun `rate below MIN clamps to MIN`() {
        // 0.1 Hz would mathematically map to 5000 ms; the clamp at
        // MIN_RATE_HZ (1) caps the half period at 500 ms.
        assertEquals(500L, StrobeService.halfPeriodMillis(0.1f))
    }

    @Test
    fun `every supported rate produces a positive delay`() {
        for (rateHz in 1..20) {
            val half = StrobeService.halfPeriodMillis(rateHz.toFloat())
            assertTrue(half > 0L, "halfPeriodMillis($rateHz) was $half")
        }
    }
}
