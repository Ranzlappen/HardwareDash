package dev.ranzlappen.gadget.core.automation.model

import dev.ranzlappen.gadget.core.automation.engine.RuleEvaluator
import dev.ranzlappen.gadget.core.automation.engine.referencedMetricKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * W7 additions: the nested [Condition.Group] node (model + evaluator +
 * wire-format pin) and the [AutomationTransfer] / [RuleTemplates] surfaces.
 */
class ConditionGroupAndTransferTest {

    private val json = AutomationJson
    private val evaluator = RuleEvaluator()

    private fun ruleWith(conditions: List<Condition>, logic: ConditionLogic) = Rule(
        id = "id",
        name = "grouped",
        trigger = Trigger.Manual,
        conditions = conditions,
        conditionLogic = logic,
        actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
    )

    // ─── wire format ────────────────────────────────────────────────

    @Test
    fun conditionGroup_roundTrips() {
        val group: Condition = Condition.Group(
            logic = ConditionLogic.Any,
            children = listOf(
                Condition.MetricCompare("battery_level", ComparisonOp.Lt, 20f),
                Condition.TimeWindow(startMinutes = 60, endMinutes = 120),
            ),
        )
        val decoded = json.decodeFromString(
            Condition.serializer(),
            json.encodeToString(Condition.serializer(), group),
        )
        assertEquals(group, decoded)
    }

    @Test
    fun conditionGroup_pinsDiscriminator() {
        val encoded = json.encodeToString(
            Condition.serializer(),
            Condition.Group(children = emptyList()),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Condition.Group\""),
        )
    }

    // ─── evaluator recursion ────────────────────────────────────────

    @Test
    fun nestedGroup_foldsWithItsOwnLogic() {
        // top-level ALL of [ A , (B OR C) ]
        val rule = ruleWith(
            conditions = listOf(
                Condition.MetricCompare("a", ComparisonOp.Gt, 10f),
                Condition.Group(
                    logic = ConditionLogic.Any,
                    children = listOf(
                        Condition.MetricCompare("b", ComparisonOp.Gt, 10f),
                        Condition.MetricCompare("c", ComparisonOp.Gt, 10f),
                    ),
                ),
            ),
            logic = ConditionLogic.All,
        )
        // a passes, b fails, c passes → (b OR c) true → ALL true
        val fired = evaluator.evaluate(
            rule = rule,
            firedTrigger = Trigger.Manual,
            readings = mapOf("a" to 20f, "b" to 0f, "c" to 20f),
            now = LocalTime.NOON,
            rootAvailable = false,
            sinceLastFiredMillis = null,
        )
        assertEquals(1, fired.size)

        // a passes but both b and c fail → (b OR c) false → ALL false
        val notFired = evaluator.evaluate(
            rule = rule,
            firedTrigger = Trigger.Manual,
            readings = mapOf("a" to 20f, "b" to 0f, "c" to 0f),
            now = LocalTime.NOON,
            rootAvailable = false,
            sinceLastFiredMillis = null,
        )
        assertTrue(notFired.isEmpty())
    }

    @Test
    fun emptyGroup_isVacuouslyTrue() {
        val rule = ruleWith(
            conditions = listOf(Condition.Group(logic = ConditionLogic.Any, children = emptyList())),
            logic = ConditionLogic.All,
        )
        val fired = evaluator.evaluate(
            rule = rule,
            firedTrigger = Trigger.Manual,
            readings = emptyMap(),
            now = LocalTime.NOON,
            rootAvailable = false,
            sinceLastFiredMillis = null,
        )
        assertEquals(1, fired.size)
    }

    @Test
    fun referencedMetricKeys_recursesIntoGroups() {
        val group = Condition.Group(
            children = listOf(
                Condition.MetricCompare("a", ComparisonOp.Gt, 1f),
                Condition.Group(
                    children = listOf(Condition.MetricCompare("b", ComparisonOp.Gt, 1f)),
                ),
                Condition.TimeWindow(0, 60),
            ),
        )
        assertEquals(listOf("a", "b"), group.referencedMetricKeys())
    }

    // ─── export / import ────────────────────────────────────────────

    @Test
    fun exportImport_roundTripsRules() {
        val rules = RuleTemplates.all.mapIndexed { i, t -> t.create("id-$i") }
        val result = AutomationTransfer.import(AutomationTransfer.export(rules))
        assertTrue(result is AutomationTransfer.ImportResult.Success)
        assertEquals(rules, (result as AutomationTransfer.ImportResult.Success).rules)
    }

    @Test
    fun import_toleratesBareRuleArray() {
        val rules = listOf(RuleTemplates.all.first().create("only"))
        val bareArray = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(Rule.serializer()),
            rules,
        )
        val result = AutomationTransfer.import(bareArray)
        assertTrue(result is AutomationTransfer.ImportResult.Success)
        assertEquals(rules, (result as AutomationTransfer.ImportResult.Success).rules)
    }

    @Test
    fun import_failsGracefullyOnGarbage() {
        assertTrue(AutomationTransfer.import("not json") is AutomationTransfer.ImportResult.Failure)
        assertTrue(AutomationTransfer.import("") is AutomationTransfer.ImportResult.Failure)
    }

    @Test
    fun templates_createDistinctIdsAndAreNormalizable() {
        val a = RuleTemplates.all.first().create("x")
        val b = RuleTemplates.all.first().create("y")
        assertEquals("x", a.id)
        assertEquals("y", b.id)
        // template rules survive normalization unchanged (valid hysteresis sides)
        assertEquals(a.copy(id = "x"), a.normalized())
    }
}
