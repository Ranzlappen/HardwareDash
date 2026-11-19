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
    val rule = withoutRetiredActions()
    val t = rule.trigger
    if (t !is Trigger.MetricThreshold) return rule
    val normalizedTrigger = t.normalizedClearValue()
    return if (normalizedTrigger == t) rule else rule.copy(trigger = normalizedTrigger)
}

/**
 * Action keys removed in v3 (MASTER-PLAN B6). They were read-only `assert_*` /
 * `*_check` "actions" that only returned Success/Failure — and `RuleFireExecutor`
 * dispatches every action without looking at the result, so they never gated
 * anything. Gating on a reading is what `Condition.MetricCompare` and
 * `Trigger.MetricThreshold` are for.
 *
 * Saved rules may still list them: [withoutRetiredActions] drops them on read
 * and on save, so they never reach the builder UI or the dispatcher. Keep this
 * set — it is the migration for rules saved (or restored from backups) before
 * v3.
 */
val RetiredActionKeys: Set<String> = setOf(
    "acceleration_assert_above",
    "acceleration_assert_below",
    "actuators_assert_available",
    "ambient_assert_bright",
    "ambient_assert_dark",
    "assert_motion_detected",
    "assert_motion_idle",
    "assert_rotation_above",
    "assert_steps_above",
    "bt_connect_check",
    "bugreport_assert_adb",
    "bugreport_assert_permission",
    "cell_assert_signal_above",
    "cell_assert_signal_below",
    "flipper_assert_connected",
    "light_assert_bright",
    "light_assert_dark",
    "lock_assert_locked",
    "lock_assert_secure",
    "lock_assert_unlocked",
    "logbook_assert_open_below",
    "notification_assert_channel_importance",
    "proximity_assert_far",
    "proximity_assert_near",
    "storage_assert_free_space",
    "subghz_bridge_check",
    "subghz_capable_check",
    "wifi_connected_check",
    "wifi_enabled_check",
)

/** [this] without any action whose key is in [RetiredActionKeys]. */
fun Rule.withoutRetiredActions(): Rule =
    if (actions.none { it.actionKey in RetiredActionKeys }) {
        this
    } else {
        copy(actions = actions.filterNot { it.actionKey in RetiredActionKeys })
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
