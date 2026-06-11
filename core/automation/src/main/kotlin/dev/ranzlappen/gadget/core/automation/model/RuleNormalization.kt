package dev.ranzlappen.gadget.core.automation.model

/**
 * Hysteresis-side validation (engine-core review P2).
 *
 * `MetricThresholdGate`'s uniform re-arm formula is correct for a
 * `clearValue` on the **re-arm side** of `value` for the trigger's
 * (op, edge) — but a clearValue on the *wrong* side silently degenerates
 * hysteresis to roughly `null` (e.g. Falling `Lt 5` with `clearValue 8`
 * re-arms below 8, so noise dithering 4.9 ↔ 5.1 machine-guns exactly as if
 * unprotected). Persisting that is a footgun, so [normalized] nulls the
 * clearValue instead — `null` is the honest semantic of a wrong-side bound
 * (plain "re-arm when the predicate goes false").
 *
 * Valid side, derived from the fire predicate (the op for [Edge.Rising],
 * its negation for [Edge.Falling]): hysteresis must make re-arming
 * *stricter* than the trigger value, i.e. the clearValue lies strictly
 * beyond `value` on the non-fire side:
 *
 * | op        | Rising requires      | Falling requires     |
 * |-----------|----------------------|----------------------|
 * | Lt / Lte  | `clearValue > value` | `clearValue < value` |
 * | Gt / Gte  | `clearValue < value` | `clearValue > value` |
 * | Eq / Neq  | no meaningful hysteresis → always nulled    |
 *
 * The builder UI should additionally restrict threshold ops to the
 * inequalities — float `Eq`/`Neq` on analog signals is its own footgun —
 * but normalization here protects every other write path (restored
 * backups, future APIs).
 */
fun Rule.normalized(): Rule {
    val t = trigger
    if (t !is Trigger.MetricThreshold) return this
    val normalizedTrigger = t.normalizedClearValue()
    return if (normalizedTrigger == t) this else copy(trigger = normalizedTrigger)
}

/** [Trigger.MetricThreshold] with an invalid-side [Trigger.MetricThreshold.clearValue] nulled. */
fun Trigger.MetricThreshold.normalizedClearValue(): Trigger.MetricThreshold {
    val clear = clearValue ?: return this
    val validSide = when (op) {
        ComparisonOp.Eq, ComparisonOp.Neq -> false
        ComparisonOp.Lt, ComparisonOp.Lte -> when (edge) {
            Edge.Rising -> clear > value
            Edge.Falling -> clear < value
        }
        ComparisonOp.Gt, ComparisonOp.Gte -> when (edge) {
            Edge.Rising -> clear < value
            Edge.Falling -> clear > value
        }
    }
    return if (validSide) this else copy(clearValue = null)
}
