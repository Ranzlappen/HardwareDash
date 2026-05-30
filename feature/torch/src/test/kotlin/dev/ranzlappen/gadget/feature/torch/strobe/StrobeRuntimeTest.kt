package dev.ranzlappen.gadget.feature.torch.strobe

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Round-trip test for the [StrobeRuntime] singleton's `StateFlow` surface —
 * the source of truth widget providers + the ViewModel read instead of
 * polling the old `@Volatile StrobeService.isRunning` companion flag.
 */
class StrobeRuntimeTest {

    @Test
    fun `initial state is not running`() {
        val runtime = StrobeRuntime()
        assertEquals(false, runtime.running.value)
    }

    @Test
    fun `setRunning publishes the new value`() {
        val runtime = StrobeRuntime()
        runtime.setRunning(true)
        assertEquals(true, runtime.running.value)
        runtime.setRunning(false)
        assertEquals(false, runtime.running.value)
    }

    @Test
    fun `setRunning is idempotent`() {
        val runtime = StrobeRuntime()
        runtime.setRunning(true)
        runtime.setRunning(true)
        assertEquals(true, runtime.running.value)
        runtime.setRunning(false)
        runtime.setRunning(false)
        assertEquals(false, runtime.running.value)
    }
}
