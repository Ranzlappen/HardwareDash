package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.ConditionLogic
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.Trigger
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The pure rule evaluator (ADR-0002 Decision 7): no Android, no I/O, no
 * coroutines — a pure function of its inputs, so correctness is locked down
 * by exhaustive JVM tests with no emulator.
 *
 * The runtime detects trigger *events* (threshold crossings via
 * [MetricThresholdGate], alarms, broadcasts) and calls [evaluate] with the
 * fired trigger plus a snapshot of the current readings; the evaluator
 * decides which actions to dispatch. Evaluation order, cheapest first:
 *
 *  1. disabled rule → empty
 *  2. **cooldown** (ADR-0002 Decision 8): `cooldownSeconds > 0` and the
 *     rule fired less than `cooldownSeconds * 1000` ms ago → empty, before
 *     any trigger/condition work. **[Trigger.Manual] rules bypass this
 *     check** — an explicit "run now" tap is consent, not an automated
 *     storm — though the runtime still records the fire (`markFired`), so a
 *     manual run delays the next automatic fire.
 *  3. fired trigger must equal the rule's trigger → else empty
 *  4. conditions fold per [ConditionLogic] (vacuously true when the list
 *     is empty, for ALL and ANY alike)
 *  5. root gating (layer 2 of 3 — see `docs/automation-engine.md` § Safety):
 *     [RuleAction.requiresRoot] actions are dropped when [rootAvailable]
 *     is false. This protects rules restored from a rooted device's backup
 *     onto a standard install.
 */
@Singleton
class RuleEvaluator @Inject constructor() {

    /**
     * @param sinceLastFiredMillis ms since this rule last fired, `null` if
     *   it never fired. The repository derives this from the persisted
     *   `last_fired_at` column so cooldowns survive process death.
     */
    fun evaluate(
        rule: Rule,
        firedTrigger: Trigger,
        readings: Map<String, Float>,
        now: LocalTime,
        rootAvailable: Boolean,
        sinceLastFiredMillis: Long?,
    ): List<RuleAction> {
        if (!rule.enabled) return emptyList()
        // Cooldown gates AUTOMATED triggers only. A Manual "run now" is
        // explicit, human-rate-limited consent, so it bypasses the cooldown
        // *check* — but the runtime still calls markFired afterwards, so a
        // manual run delays the next automatic fire (ADR-0002 Decision 8 /
        // the Batch-F decision; see docs/automation-engine.md § Runtime host).
        val cooldownApplies = rule.cooldownSeconds > 0 && rule.trigger !is Trigger.Manual
        if (cooldownApplies &&
            sinceLastFiredMillis != null &&
            sinceLastFiredMillis < rule.cooldownSeconds * 1_000L
        ) {
            return emptyList()
        }
        if (firedTrigger != rule.trigger) return emptyList()

        val conditionsHold = when (rule.conditionLogic) {
            ConditionLogic.All -> rule.conditions.all { it.holds(readings, now) }
            ConditionLogic.Any ->
                rule.conditions.isEmpty() || rule.conditions.any { it.holds(readings, now) }
        }
        if (!conditionsHold) return emptyList()

        return rule.actions.filter { !it.requiresRoot || rootAvailable }
    }
}

/**
 * Whether this condition holds against the [readings] snapshot at [now].
 *
 * A [Condition.MetricCompare] whose metric is **absent from the snapshot
 * fails** (safe default: an unreadable signal never satisfies a gate).
 *
 * A [Condition.TimeWindow] is inclusive of [Condition.TimeWindow.startMinutes]
 * and exclusive of [Condition.TimeWindow.endMinutes]; `end < start` wraps
 * midnight (22:00–06:00 = late evening through early morning); and
 * `start == end` means the **full day** (a zero-length window would be
 * unsatisfiable and is never what a user means).
 */
internal fun Condition.holds(readings: Map<String, Float>, now: LocalTime): Boolean = when (this) {
    is Condition.MetricCompare -> readings[metricKey]?.let { op.compare(it, value) } ?: false
    is Condition.TimeWindow -> {
        val minutes = now.hour * 60 + now.minute
        when {
            startMinutes == endMinutes -> true
            startMinutes < endMinutes -> minutes >= startMinutes && minutes < endMinutes
            else -> minutes >= startMinutes || minutes < endMinutes
        }
    }
}

/** `sample <op> bound`, e.g. `Lt.compare(3f, 5f) == true`. */
internal fun ComparisonOp.compare(sample: Float, bound: Float): Boolean = when (this) {
    ComparisonOp.Lt -> sample < bound
    ComparisonOp.Lte -> sample <= bound
    ComparisonOp.Gt -> sample > bound
    ComparisonOp.Gte -> sample >= bound
    ComparisonOp.Eq -> sample == bound
    ComparisonOp.Neq -> sample != bound
}
