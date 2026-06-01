package dev.ranzlappen.gadget.feature.vibration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the modelled-signal semantics of [VibrationRuntime]: timed commands
 * decay to 0, sustained commands hold, replace-on-new cancels a prior decay,
 * and [clear] resets. The decay coroutine runs on a real dispatcher, so the
 * decay assertions poll within a bounded deadline.
 */
class VibrationRuntimeTest {

    private fun awaitAmplitude(runtime: VibrationRuntime, expected: Int, timeoutMs: Long = 2_000L) {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        while (runtime.state.value.amplitudePercent != expected && System.nanoTime() < deadline) {
            Thread.sleep(2)
        }
        assertEquals(expected, runtime.state.value.amplitudePercent)
    }

    @Test
    fun `timed command decays to zero after its duration`() {
        val runtime = VibrationRuntime()
        runtime.setCommand(amplitudePercent = 80, durationMillis = 50L)
        assertEquals(80, runtime.state.value.amplitudePercent)
        assertTrue(runtime.state.value.isActive)
        awaitAmplitude(runtime, 0)
        assertTrue(!runtime.state.value.isActive)
    }

    @Test
    fun `sustained command holds until cleared`() = runBlocking {
        val runtime = VibrationRuntime()
        runtime.setSustained(60)
        delay(80)
        assertEquals(60, runtime.state.value.amplitudePercent)
        runtime.clear()
        assertEquals(0, runtime.state.value.amplitudePercent)
    }

    @Test
    fun `a new command replaces a prior decay`() {
        val runtime = VibrationRuntime()
        runtime.setCommand(amplitudePercent = 50, durationMillis = 30L)
        // Before the first decay fires, issue a longer command — the first
        // decay must be cancelled so it can't zero the new value early.
        runtime.setCommand(amplitudePercent = 90, durationMillis = 1_000L)
        assertEquals(90, runtime.state.value.amplitudePercent)
        Thread.sleep(60) // past the first (cancelled) decay deadline
        assertEquals(90, runtime.state.value.amplitudePercent)
    }

    @Test
    fun `command clamps to the supplied ceiling`() {
        val runtime = VibrationRuntime()
        runtime.setSustained(amplitudePercent = 150, ceiling = 100)
        assertEquals(100, runtime.state.value.amplitudePercent)
    }
}
