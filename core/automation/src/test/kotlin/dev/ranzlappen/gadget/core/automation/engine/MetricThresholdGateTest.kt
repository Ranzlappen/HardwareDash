package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Threshold edge + hysteresis arm/re-arm sequences from the design doc's
 * named JVM test list: fire at `value`, suppressed until the reading
 * crosses `clearValue`, then re-arm. The canonical sequence is the doc's
 * proximity example: `Lt 5` with `clearValue = 8`.
 */
class MetricThresholdGateTest {

    private val proximity = Trigger.MetricThreshold(
        metricKey = "proximity",
        op = ComparisonOp.Lt,
        value = 5f,
        edge = Edge.Rising,
        clearValue = 8f,
    )

    /** Feed [samples] from [initial], returning the fire flags per sample. */
    private fun run(
        trigger: Trigger.MetricThreshold,
        initial: MetricThresholdGate.State,
        vararg samples: Float,
    ): List<Boolean> {
        var state = initial
        return samples.map { sample ->
            val step = MetricThresholdGate.step(trigger, state, sample)
            state = step.state
            step.fire
        }
    }

    private fun armed() = MetricThresholdGate.State(armed = true)

    // ─── edges fire once, not per matching sample ───────────────────

    @Test
    fun rising_firesOnceOnEntering_notEverySample() {
        // 10 (outside) → 4 (fires) → 3, 2 (inside; must NOT re-fire)
        assertEquals(listOf(false, true, false, false), run(proximity, armed(), 10f, 4f, 3f, 2f))
    }

    @Test
    fun opBoundary_strictLt_doesNotFireAtValue() {
        assertEquals(listOf(false), run(proximity, armed(), 5f))
    }

    @Test
    fun opBoundary_lte_firesAtValue() {
        val lte = proximity.copy(op = ComparisonOp.Lte)
        assertEquals(listOf(true), run(lte, armed(), 5f))
    }

    @Test
    fun falling_firesOnLeavingThePredicate() {
        // Falling on Lt 5 = fire when the reading leaves "< 5".
        val falling = Trigger.MetricThreshold(
            metricKey = "proximity",
            op = ComparisonOp.Lt,
            value = 5f,
            edge = Edge.Falling,
            clearValue = 3f, // re-arm only once firmly back inside (< 3)
        )
        // start inside (armed via initialState), leave → fire
        val initial = MetricThresholdGate.initialState(falling, firstSample = 2f)
        assertTrue(initial.armed)
        assertEquals(listOf(false, true, false), run(falling, initial, 4f, 6f, 7f))
    }

    // ─── hysteresis: the canonical proximity sequence ────────────────

    @Test
    fun hysteresis_noiseAroundThreshold_cannotMachineGun() {
        // Fire at 4. Noise dithering 4 ↔ 6 around the 5 threshold must not
        // re-fire: the re-arm bound is 8, not 5.
        assertEquals(
            listOf(true, false, false, false, false),
            run(proximity, armed(), 4f, 6f, 4f, 6f, 4f),
        )
    }

    @Test
    fun hysteresis_rearmsPastClearValue_thenFiresAgain() {
        // 4 fires → 9 re-arms (≥ 8) → 4 fires again.
        assertEquals(listOf(true, false, true), run(proximity, armed(), 4f, 9f, 4f))
    }

    @Test
    fun hysteresis_atExactlyClearValue_rearms() {
        // One uniform rule: at-the-bound counts as re-armed (!(8 < 8)).
        assertEquals(listOf(true, false, true), run(proximity, armed(), 4f, 8f, 4f))
    }

    @Test
    fun nullClearValue_rearmsAsSoonAsPredicateGoesFalse() {
        val noHysteresis = proximity.copy(clearValue = null)
        // 4 fires → 6 re-arms (predicate false) → 4 fires again.
        assertEquals(listOf(true, false, true), run(noHysteresis, armed(), 4f, 6f, 4f))
    }

    // ─── initial state: a trigger is an edge, not a level ───────────

    @Test
    fun initialState_insidePredicate_startsDisarmed() {
        // Proximity already < 5 when the rule is created: no fire-on-subscribe.
        val initial = MetricThresholdGate.initialState(proximity, firstSample = 2f)
        assertFalse(initial.armed)
        // Stays silent inside; re-arms past clearValue; then fires on re-entry.
        assertEquals(listOf(false, false, true), run(proximity, initial, 3f, 9f, 4f))
    }

    @Test
    fun initialState_outsidePredicate_startsArmed() {
        val initial = MetricThresholdGate.initialState(proximity, firstSample = 20f)
        assertTrue(initial.armed)
    }

    // ─── Gt mirror (the formula is symmetric) ───────────────────────

    @Test
    fun gtThreshold_withLowerClearValue() {
        // Battery temp Gt 40, clearValue 35: fires above 40, re-arms ≤ 35.
        val temp = Trigger.MetricThreshold(
            metricKey = "battery_temp",
            op = ComparisonOp.Gt,
            value = 40f,
            edge = Edge.Rising,
            clearValue = 35f,
        )
        assertEquals(
            listOf(true, false, false, false, true),
            run(temp, armed(), 41f, 39f, 41f, 35f, 41f),
        )
    }
}
