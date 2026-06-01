package dev.ranzlappen.gadget.feature.vibration.monitor

import android.content.Context
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRuntime
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [VibrationMetricSource] — the non-pollable-actuator stress
 * test of the `MetricSource` poll contract. Verifies the live ceiling wiring
 * (a constant 100 on standard; follows the rooted ceiling flow) and the
 * modelled-signal sampling (idle → 0, commanded → percent, rooted-commanded
 * takes precedence).
 */
class VibrationMetricSourceTest {

    private fun newSource(
        runtime: VibrationRuntime,
        maxFlow: MutableStateFlow<Int> = MutableStateFlow(100),
        commanded: MutableStateFlow<Int> = MutableStateFlow(0),
    ): VibrationMetricSource {
        val context = mockk<Context>(relaxed = true)
        val caps = mockk<VibrationRootCapabilities>(relaxed = true)
        every { caps.maxAmplitudePercentFlow } returns maxFlow
        every { caps.commandedAmplitudePercent } returns commanded
        return VibrationMetricSource(context, runtime, caps)
    }

    private fun assertMaxReaches(source: VibrationMetricSource, expected: Float) {
        val deadlineNanos = System.nanoTime() + 2_000_000_000L
        while (source.descriptor.currentMax() != expected && System.nanoTime() < deadlineNanos) {
            Thread.sleep(2)
        }
        assertEquals(expected, source.descriptor.currentMax())
    }

    @Test
    fun `standard build reports a constant 100 percent ceiling`() {
        val source = newSource(VibrationRuntime())
        assertMaxReaches(source, 100f)
    }

    @Test
    fun `rooted ceiling raises the metric max`() {
        val maxFlow = MutableStateFlow(100)
        val source = newSource(VibrationRuntime(), maxFlow = maxFlow)
        assertMaxReaches(source, 100f)
        maxFlow.value = 150
        assertMaxReaches(source, 150f)
    }

    @Test
    fun `idle runtime samples zero`() = runBlocking {
        val source = newSource(VibrationRuntime())
        assertEquals(0f, source.sample())
    }

    @Test
    fun `commanded amplitude is sampled`() = runBlocking {
        val runtime = VibrationRuntime()
        val source = newSource(runtime)
        runtime.setSustained(70)
        assertEquals(70f, source.sample())
    }

    @Test
    fun `rooted commanded amplitude takes precedence over runtime`() = runBlocking {
        val runtime = VibrationRuntime().apply { setSustained(40) }
        val commanded = MutableStateFlow(0)
        val source = newSource(runtime, commanded = commanded)
        assertEquals(40f, source.sample())
        commanded.value = 130 // rooted extreme-amplitude in flight
        assertEquals(130f, source.sample())
    }
}
