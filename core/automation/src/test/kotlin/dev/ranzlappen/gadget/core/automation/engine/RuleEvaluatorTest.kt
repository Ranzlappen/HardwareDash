package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.ConditionLogic
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The design doc's named JVM test list for the evaluator (batch 3.2
 * acceptance): enabled/disabled, ALL/ANY folding, time windows that wrap
 * midnight, cooldown just-under / exactly-at / just-over, and root-gated
 * actions filtered on standard. Threshold edge + hysteresis sequences live
 * in [MetricThresholdGateTest].
 */
class RuleEvaluatorTest {

    private val evaluator = RuleEvaluator()

    private val manualTrigger = Trigger.Manual
    // An automated trigger for cooldown tests — Manual now BYPASSES cooldown
    // (Batch-F decision), so cooldown enforcement must be exercised on a
    // non-Manual trigger.
    private val automatedTrigger = Trigger.SystemEvent(SystemEventKind.PowerConnected)
    private val torchOff = RuleAction(featureId = "torch", actionKey = "off")
    private val rootAction = RuleAction(featureId = "torch", actionKey = "boost", requiresRoot = true)

    private fun rule(
        enabled: Boolean = true,
        trigger: Trigger = manualTrigger,
        conditions: List<Condition> = emptyList(),
        conditionLogic: ConditionLogic = ConditionLogic.All,
        actions: List<RuleAction> = listOf(torchOff),
        cooldownSeconds: Int = 0,
    ) = Rule(
        id = "r1",
        name = "test",
        enabled = enabled,
        trigger = trigger,
        conditions = conditions,
        conditionLogic = conditionLogic,
        actions = actions,
        cooldownSeconds = cooldownSeconds,
    )

    private fun evaluate(
        rule: Rule,
        firedTrigger: Trigger = rule.trigger,
        readings: Map<String, Float> = emptyMap(),
        now: LocalTime = LocalTime.NOON,
        rootAvailable: Boolean = false,
        sinceLastFiredMillis: Long? = null,
    ) = evaluator.evaluate(rule, firedTrigger, readings, now, rootAvailable, sinceLastFiredMillis)

    // ─── enabled / trigger match ────────────────────────────────────

    @Test
    fun disabledRule_returnsEmpty() {
        assertEquals(emptyList<RuleAction>(), evaluate(rule(enabled = false)))
    }

    @Test
    fun enabledRule_noConditions_dispatchesActions() {
        assertEquals(listOf(torchOff), evaluate(rule()))
    }

    @Test
    fun nonMatchingTrigger_returnsEmpty() {
        val r = rule(trigger = Trigger.SystemEvent(SystemEventKind.PowerConnected))
        assertEquals(emptyList<RuleAction>(), evaluate(r, firedTrigger = Trigger.Manual))
    }

    // ─── cooldown boundaries (just-under / exactly-at / just-over) ──

    @Test
    fun cooldown_justUnder_suppresses() {
        val r = rule(trigger = automatedTrigger, cooldownSeconds = 30)
        assertEquals(emptyList<RuleAction>(), evaluate(r, sinceLastFiredMillis = 29_999L))
    }

    @Test
    fun cooldown_exactlyAt_fires() {
        // The suppression window is strictly `< cooldownSeconds * 1000`.
        val r = rule(trigger = automatedTrigger, cooldownSeconds = 30)
        assertEquals(listOf(torchOff), evaluate(r, sinceLastFiredMillis = 30_000L))
    }

    @Test
    fun cooldown_justOver_fires() {
        val r = rule(trigger = automatedTrigger, cooldownSeconds = 30)
        assertEquals(listOf(torchOff), evaluate(r, sinceLastFiredMillis = 30_001L))
    }

    @Test
    fun cooldown_neverFired_fires() {
        val r = rule(trigger = automatedTrigger, cooldownSeconds = 30)
        assertEquals(listOf(torchOff), evaluate(r, sinceLastFiredMillis = null))
    }

    @Test
    fun zeroCooldown_ignoresLastFired() {
        assertEquals(listOf(torchOff), evaluate(rule(), sinceLastFiredMillis = 1L))
    }

    // ─── Manual bypasses cooldown (Batch-F decision) ────────────────

    @Test
    fun manualTrigger_bypassesCooldown_evenInsideTheWindow() {
        // rule()'s trigger is Manual; a tap inside the cooldown window fires.
        val r = rule(cooldownSeconds = 30)
        assertEquals(listOf(torchOff), evaluate(r, sinceLastFiredMillis = 1L))
    }

    @Test
    fun automatedTrigger_stillObeysCooldown() {
        // The same cooldown on a non-Manual trigger is enforced.
        val r = rule(
            trigger = Trigger.SystemEvent(SystemEventKind.PowerConnected),
            cooldownSeconds = 30,
        )
        assertEquals(
            emptyList<RuleAction>(),
            evaluate(r, sinceLastFiredMillis = 1L),
        )
    }

    // ─── condition folding: ALL / ANY ───────────────────────────────

    private val batteryAbove20 =
        Condition.MetricCompare(metricKey = "battery_level", op = ComparisonOp.Gt, value = 20f)
    private val proximityNear =
        Condition.MetricCompare(metricKey = "proximity", op = ComparisonOp.Lt, value = 5f)

    @Test
    fun allLogic_everyConditionMustHold() {
        val r = rule(conditions = listOf(batteryAbove20, proximityNear), conditionLogic = ConditionLogic.All)
        val pass = mapOf("battery_level" to 50f, "proximity" to 2f)
        val oneFails = mapOf("battery_level" to 50f, "proximity" to 9f)
        assertEquals(listOf(torchOff), evaluate(r, readings = pass))
        assertEquals(emptyList<RuleAction>(), evaluate(r, readings = oneFails))
    }

    @Test
    fun anyLogic_oneConditionSuffices() {
        val r = rule(conditions = listOf(batteryAbove20, proximityNear), conditionLogic = ConditionLogic.Any)
        val oneHolds = mapOf("battery_level" to 10f, "proximity" to 2f)
        val noneHold = mapOf("battery_level" to 10f, "proximity" to 9f)
        assertEquals(listOf(torchOff), evaluate(r, readings = oneHolds))
        assertEquals(emptyList<RuleAction>(), evaluate(r, readings = noneHold))
    }

    @Test
    fun emptyConditions_vacuouslyTrue_forBothLogics() {
        assertEquals(listOf(torchOff), evaluate(rule(conditionLogic = ConditionLogic.All)))
        assertEquals(listOf(torchOff), evaluate(rule(conditionLogic = ConditionLogic.Any)))
    }

    @Test
    fun missingReading_failsTheCondition() {
        // A metric absent from the snapshot never satisfies a gate.
        val r = rule(conditions = listOf(batteryAbove20))
        assertEquals(emptyList<RuleAction>(), evaluate(r, readings = emptyMap()))
    }

    // ─── time windows (incl. midnight wrap) ─────────────────────────

    private fun windowRule(start: Int, end: Int) =
        rule(conditions = listOf(Condition.TimeWindow(startMinutes = start, endMinutes = end)))

    @Test
    fun timeWindow_simple_insideAndOutside() {
        val r = windowRule(start = 9 * 60, end = 17 * 60)
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(12, 0)))
        assertEquals(emptyList<RuleAction>(), evaluate(r, now = LocalTime.of(18, 0)))
        // start inclusive, end exclusive
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(9, 0)))
        assertEquals(emptyList<RuleAction>(), evaluate(r, now = LocalTime.of(17, 0)))
    }

    @Test
    fun timeWindow_wrapsMidnight() {
        val r = windowRule(start = 22 * 60, end = 6 * 60) // 22:00 → 06:00
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(23, 30)))
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(2, 0)))
        assertEquals(emptyList<RuleAction>(), evaluate(r, now = LocalTime.of(12, 0)))
        // boundaries: start inclusive, end exclusive
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(22, 0)))
        assertEquals(emptyList<RuleAction>(), evaluate(r, now = LocalTime.of(6, 0)))
    }

    @Test
    fun timeWindow_startEqualsEnd_meansFullDay() {
        val r = windowRule(start = 9 * 60, end = 9 * 60)
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(9, 0)))
        assertEquals(listOf(torchOff), evaluate(r, now = LocalTime.of(21, 0)))
    }

    // ─── root gating (layer 2) ──────────────────────────────────────

    @Test
    fun rootAction_droppedWhenRootUnavailable() {
        val r = rule(actions = listOf(torchOff, rootAction))
        assertEquals(listOf(torchOff), evaluate(r, rootAvailable = false))
    }

    @Test
    fun rootAction_keptWhenRootAvailable() {
        val r = rule(actions = listOf(torchOff, rootAction))
        assertEquals(listOf(torchOff, rootAction), evaluate(r, rootAvailable = true))
    }

    @Test
    fun rootOnlyRule_onStandard_dispatchesNothing() {
        // A rule restored from a rooted device's backup onto standard.
        val r = rule(actions = listOf(rootAction))
        assertTrue(evaluate(r, rootAvailable = false).isEmpty())
    }
}
