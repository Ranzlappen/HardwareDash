package dev.ranzlappen.gadget.feature.torch.monitor

import android.content.Context
import dev.ranzlappen.gadget.core.model.currentMax
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the **live ceiling** wiring in [TorchMetricSource]. The
 * metric is flavor-agnostic — it only reads the injected
 * [TorchRootCapabilities.maxBrightnessPercentFlow] — so each flavor/probe
 * scenario is reproduced by driving that flow directly:
 *
 *  - **Standard build** — a constant 100% ceiling: `currentMax() == 100f`.
 *  - **Rooted build, successful probe** — the capabilities flow flips to the
 *    boost cap (150) once the LED node resolves: `currentMax()` follows to
 *    `150f`.
 *  - **Rooted build, failed probe** — no usable LED node, so the capabilities
 *    flow never leaves 100: `currentMax()` stays `100f`.
 *
 * The descriptor's `maxFlow` is a `stateIn(Eagerly)` derivation collected on a
 * real dispatcher, so the rooted-success assertion polls within a bounded
 * deadline rather than assuming synchronous propagation.
 */
class TorchMetricSourceTest {

    private fun newSource(maxFlow: MutableStateFlow<Int>): TorchMetricSource {
        val context = mockk<Context>(relaxed = true)
        val controller = mockk<TorchController>(relaxed = true)
        val capabilities = mockk<TorchRootCapabilities>(relaxed = true)
        every { capabilities.maxBrightnessPercentFlow } returns maxFlow
        return TorchMetricSource(context, controller, capabilities)
    }

    /** Polls [TorchMetricSource.descriptor]'s live ceiling until it reaches
     *  [expected] or a 2s deadline elapses, then asserts the final value. */
    private fun assertMaxReaches(source: TorchMetricSource, expected: Float) {
        val deadlineNanos = System.nanoTime() + 2_000_000_000L
        while (source.descriptor.currentMax() != expected && System.nanoTime() < deadlineNanos) {
            Thread.sleep(2)
        }
        assertEquals(expected, source.descriptor.currentMax())
    }

    @Test
    fun `standard build reports a constant 100 percent ceiling`() {
        val source = newSource(MutableStateFlow(100))
        assertMaxReaches(source, 100f)
    }

    @Test
    fun `rooted build with successful probe raises the ceiling to 150`() {
        val maxFlow = MutableStateFlow(100)
        val source = newSource(maxFlow)
        // Before the probe resolves the ceiling is the stock 100%.
        assertMaxReaches(source, 100f)

        // A successful probe (root + usable LED node) flips the capabilities
        // flow to the boost cap; the descriptor's maxFlow must follow.
        maxFlow.value = 150
        assertMaxReaches(source, 150f)
    }

    @Test
    fun `rooted build with failed probe keeps the ceiling at 100`() {
        // A failed probe (no /sys/class/leds node) never raises the
        // capabilities flow, so the ceiling stays at the stock 100%.
        val source = newSource(MutableStateFlow(100))
        assertMaxReaches(source, 100f)
    }
}
