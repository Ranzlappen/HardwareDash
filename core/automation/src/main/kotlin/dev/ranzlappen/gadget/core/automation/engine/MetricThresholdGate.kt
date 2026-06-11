package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Trigger

/**
 * The pure arm/fire state machine behind [Trigger.MetricThreshold]. The
 * runtime feeds it each metric sample; it decides when the trigger *fires*
 * (an edge event) versus merely *matches* (every sample inside the
 * predicate). Stateless by construction — the caller carries the [State] —
 * so the whole edge/hysteresis behaviour is JVM-testable.
 *
 * Semantics (see `docs/automation-engine.md` § Trigger taxonomy):
 *
 *  - The **fire predicate** is the trigger's comparison for [Edge.Rising]
 *    ("fire on entering"), its negation for [Edge.Falling] ("fire on
 *    leaving").
 *  - A sample satisfying the fire predicate while **armed** fires exactly
 *    once and disarms.
 *  - **Re-arming**: the trigger re-arms when a sample is outside the fire
 *    predicate evaluated against the re-arm bound —
 *    [Trigger.MetricThreshold.clearValue] when set, else the trigger's own
 *    value. With `clearValue = null` that is simply "the predicate went
 *    false"; with a clearValue it is hysteresis: proximity `Lt 5` /
 *    `clearValue 8` fires below 5 and re-arms only at ≥ 8, so noise
 *    dithering around 5 cannot machine-gun the rule. (One uniform formula —
 *    at-the-bound counts as re-armed.)
 *
 * **Warning — the clearValue's side matters.** The uniform formula assumes
 * the clearValue lies on the **re-arm side** of `value` for the trigger's
 * (op, edge). A wrong-side clearValue (e.g. Falling `Lt 5` with
 * `clearValue 8`) silently degenerates hysteresis to ~`null` — dithering
 * around the threshold machine-guns as if unprotected. Every persistence
 * path must run the trigger through `normalizedClearValue()` (see
 * `RuleNormalization` in the model package; `RuleRepository.save` does) and
 * the builder UI should restrict threshold ops to inequalities.
 */
object MetricThresholdGate {

    /** Carried by the caller between samples. [armed] = ready to fire. */
    data class State(val armed: Boolean)

    /** One step's outcome: whether to fire, and the state to carry. */
    data class Step(val fire: Boolean, val state: State)

    /**
     * State for the very first sample after subscribing: armed only if the
     * sample is *outside* the fire predicate. A metric already inside the
     * predicate at subscribe time (proximity already < 5 cm when the rule is
     * created) must not fire until it leaves and re-enters — a trigger is an
     * edge, not a level.
     */
    fun initialState(trigger: Trigger.MetricThreshold, firstSample: Float): State =
        State(armed = !firePredicate(trigger, firstSample, bound = trigger.value))

    /** Advance the gate by one sample. */
    fun step(trigger: Trigger.MetricThreshold, state: State, sample: Float): Step {
        val firing = firePredicate(trigger, sample, bound = trigger.value)
        return if (state.armed) {
            if (firing) Step(fire = true, state = State(armed = false))
            else Step(fire = false, state = state)
        } else {
            val rearmBound = trigger.clearValue ?: trigger.value
            val rearmed = !firePredicate(trigger, sample, bound = rearmBound)
            Step(fire = false, state = State(armed = rearmed))
        }
    }

    private fun firePredicate(
        trigger: Trigger.MetricThreshold,
        sample: Float,
        bound: Float,
    ): Boolean {
        val inPredicate = trigger.op.compare(sample, bound)
        return when (trigger.edge) {
            Edge.Rising -> inPredicate
            Edge.Falling -> !inPredicate
        }
    }
}
