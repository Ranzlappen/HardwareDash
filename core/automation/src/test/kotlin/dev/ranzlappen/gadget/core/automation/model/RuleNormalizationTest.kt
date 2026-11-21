package dev.ranzlappen.gadget.core.automation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hysteresis-side validation (engine-core review P2): a clearValue on the
 * wrong side of value for the trigger's (op, edge) must be nulled on save,
 * never persisted. One test per cell of the validity table in
 * [normalizedClearValue]'s KDoc.
 */
class RuleNormalizationTest {

    private fun threshold(
        op: ComparisonOp,
        edge: Edge,
        value: Float = 5f,
        clear: Float?,
    ) = Trigger.MetricThreshold(
        metricKey = "m",
        op = op,
        value = value,
        edge = edge,
        clearValue = clear,
    )

    // ─── valid sides survive ────────────────────────────────────────

    @Test
    fun risingLt_clearAboveValue_kept() {
        val t = threshold(ComparisonOp.Lt, Edge.Rising, clear = 8f)
        assertEquals(8f, t.normalizedClearValue().clearValue)
    }

    @Test
    fun risingGt_clearBelowValue_kept() {
        val t = threshold(ComparisonOp.Gt, Edge.Rising, value = 40f, clear = 35f)
        assertEquals(35f, t.normalizedClearValue().clearValue)
    }

    @Test
    fun fallingLt_clearBelowValue_kept() {
        val t = threshold(ComparisonOp.Lte, Edge.Falling, clear = 3f)
        assertEquals(3f, t.normalizedClearValue().clearValue)
    }

    @Test
    fun fallingGt_clearAboveValue_kept() {
        val t = threshold(ComparisonOp.Gte, Edge.Falling, value = 40f, clear = 45f)
        assertEquals(45f, t.normalizedClearValue().clearValue)
    }

    // ─── wrong sides nulled (the review's degenerate example first) ─

    @Test
    fun fallingLt_clearAboveValue_nulled() {
        // The review's example: Falling Lt 5 with clearValue 8 re-arms below
        // 8 — dithering 4.9 ↔ 5.1 machine-guns as if unprotected.
        val t = threshold(ComparisonOp.Lt, Edge.Falling, clear = 8f)
        assertNull(t.normalizedClearValue().clearValue)
    }

    @Test
    fun risingLt_clearBelowValue_nulled() {
        assertNull(threshold(ComparisonOp.Lt, Edge.Rising, clear = 3f).normalizedClearValue().clearValue)
    }

    @Test
    fun risingGt_clearAboveValue_nulled() {
        assertNull(
            threshold(ComparisonOp.Gt, Edge.Rising, value = 40f, clear = 45f)
                .normalizedClearValue().clearValue,
        )
    }

    @Test
    fun clearEqualToValue_nulled() {
        // Equal = zero-width band = no hysteresis; null is the honest form.
        assertNull(threshold(ComparisonOp.Lt, Edge.Rising, clear = 5f).normalizedClearValue().clearValue)
    }

    @Test
    fun eqAndNeq_alwaysNulled() {
        assertNull(threshold(ComparisonOp.Eq, Edge.Rising, clear = 8f).normalizedClearValue().clearValue)
        assertNull(threshold(ComparisonOp.Neq, Edge.Falling, clear = 3f).normalizedClearValue().clearValue)
    }

    // ─── Rule.normalized plumbing ───────────────────────────────────

    @Test
    fun ruleNormalized_fixesTheTrigger() {
        val rule = Rule(
            id = "r",
            name = "n",
            trigger = threshold(ComparisonOp.Lt, Edge.Falling, clear = 8f),
        )
        val normalized = rule.normalized()
        assertNull((normalized.trigger as Trigger.MetricThreshold).clearValue)
    }

    @Test
    fun ruleNormalized_isIdentityWhenValid() {
        val rule = Rule(
            id = "r",
            name = "n",
            trigger = threshold(ComparisonOp.Lt, Edge.Rising, clear = 8f),
        )
        assertSame(rule, rule.normalized())
    }

    @Test
    fun ruleNormalized_isIdentityForNonThresholdTriggers() {
        val rule = Rule(id = "r", name = "n", trigger = Trigger.Manual)
        assertSame(rule, rule.normalized())
    }

    // ─── v3 B6: retired assert_* actions ─────────────────────────────

    @Test
    fun retiredAssertActions_areDroppedOnNormalize_andRealActionsKept() {
        val rule = Rule(
            id = "r",
            name = "n",
            trigger = Trigger.Manual,
            actions = listOf(
                RuleAction(featureId = "sensors", actionKey = "proximity_assert_near"),
                RuleAction(featureId = "torch", actionKey = "torch_on"),
                RuleAction(featureId = "lock", actionKey = "lock_assert_locked"),
                RuleAction(featureId = "wifi", actionKey = "wifi_connected_check"),
            ),
        )
        assertEquals(listOf("torch_on"), rule.normalized().actions.map { it.actionKey })
        assertEquals(listOf("torch_on"), rule.withUpgradedActions().actions.map { it.actionKey })
    }

    @Test
    fun withUpgradedActions_isIdentityWhenNothingIsRetired() {
        val rule = Rule(
            id = "r",
            name = "n",
            trigger = Trigger.Manual,
            actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_on")),
        )
        assertSame(rule, rule.withUpgradedActions())
    }

    @Test
    fun retiredKeys_areAllReadOnlyAssertsOrChecks() {
        assertEquals(29, RetiredActionKeys.size)
        RetiredActionKeys.forEach { key -> assertTrue(key, "assert" in key || key.endsWith("_check")) }
    }

    // ─── v3 B7a: moved actions ───────────────────────────────────────

    @Test
    fun actuatorHaptics_becomeVibrationEffects() {
        val rule = Rule(
            id = "r",
            name = "n",
            trigger = Trigger.Manual,
            actions = listOf(
                RuleAction(featureId = "actuators", actionKey = "actuators_haptic_click"),
                RuleAction(featureId = "actuators", actionKey = "actuators_haptic_heavy"),
                RuleAction(featureId = "torch", actionKey = "torch_on"),
            ),
        )
        val upgraded = rule.normalized().actions
        assertEquals(
            listOf("vibration" to "effect", "vibration" to "effect", "torch" to "torch_on"),
            upgraded.map { it.featureId to it.actionKey },
        )
        assertEquals("click", upgraded[0].params["effect"])
        assertEquals("heavy_click", upgraded[1].params["effect"])
    }

    @Test
    fun movedTargets_areNeverRetired() {
        MovedActions.values.forEach { target -> assertTrue(target.actionKey !in RetiredActionKeys) }
    }
}
