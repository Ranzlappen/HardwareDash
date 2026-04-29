package com.gadget.ui.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionNodeTest {

    private fun leaf(metric: String = "battery_level", op: String = "lt", t: String = "20") =
        ConditionNode.Leaf(metricKey = metric, operator = op, threshold = t)

    // ── Tree helpers ──────────────────────────────────────────────────────

    @Test fun `allLeaves flattens nested groups in order`() {
        val l1 = leaf(metric = "a")
        val l2 = leaf(metric = "b")
        val l3 = leaf(metric = "c")
        val tree = ConditionNode.Group(
            logic = LogicOperator.AND,
            children = listOf(
                l1,
                ConditionNode.Group(logic = LogicOperator.OR, children = listOf(l2, l3)),
            ),
        )
        val keys = tree.allLeaves().map { it.metricKey }
        assertEquals(listOf("a", "b", "c"), keys)
    }

    @Test fun `depth of single leaf is 1`() {
        assertEquals(1, leaf().depth())
    }

    @Test fun `depth of nested group counts levels`() {
        val tree = ConditionNode.Group(
            children = listOf(
                ConditionNode.Group(
                    children = listOf(leaf()),
                ),
            ),
        )
        assertEquals(3, tree.depth())
    }

    // ── V2 round-trip serialization ───────────────────────────────────────

    @Test fun `saveRulesV2 and loadRulesV2 round-trip preserves a leaf-only rule`() {
        val rule = LinkRuleV2(
            id = "r1",
            name = "Battery Low",
            root = ConditionNode.Group(children = listOf(leaf(metric = "battery_level", op = "lt", t = "20"))),
            actions = listOf(ActionStep(actionType = "notification", actionConfig = mapOf("title" to "Low"))),
            cooldownSec = 30,
            triggerDelaySec = 5,
            cancelDelayIfFalse = false,
        )
        val json = saveRulesV2(listOf(rule))
        val loaded = loadRulesV2(json)
        assertEquals(1, loaded.size)
        val r = loaded[0]
        assertEquals("r1", r.id)
        assertEquals("Battery Low", r.name)
        assertEquals(30, r.cooldownSec)
        assertEquals(5, r.triggerDelaySec)
        assertEquals(false, r.cancelDelayIfFalse)
        val first = (r.root as ConditionNode.Group).children.first() as ConditionNode.Leaf
        assertEquals("battery_level", first.metricKey)
        assertEquals("lt", first.operator)
        assertEquals("20", first.threshold)
    }

    @Test fun `saveRulesV2 round-trip preserves nested NOT and OR groups`() {
        val tree = ConditionNode.Group(
            logic = LogicOperator.OR,
            negate = true,
            children = listOf(
                ConditionNode.Group(
                    logic = LogicOperator.AND,
                    children = listOf(
                        leaf(metric = "battery_level", op = "lt", t = "20"),
                        leaf(metric = "battery_status", op = "eq", t = "Discharging"),
                    ),
                ),
                ConditionNode.Leaf(
                    metricKey = "ambient_temp", operator = "gt", threshold = "35",
                    negate = true, sustainSec = 60,
                ),
            ),
        )
        val rule = LinkRuleV2(
            id = "complex",
            root = tree,
            actions = listOf(ActionStep(actionType = "ring", delayMs = 2000)),
        )

        val json = saveRulesV2(listOf(rule))
        val loaded = loadRulesV2(json).first()

        val rootGroup = loaded.root as ConditionNode.Group
        assertEquals(LogicOperator.OR, rootGroup.logic)
        assertTrue(rootGroup.negate)
        assertEquals(2, rootGroup.children.size)

        val sub = rootGroup.children[0] as ConditionNode.Group
        assertEquals(LogicOperator.AND, sub.logic)
        assertEquals(2, sub.children.size)

        val tail = rootGroup.children[1] as ConditionNode.Leaf
        assertEquals("ambient_temp", tail.metricKey)
        assertTrue(tail.negate)
        assertEquals(60, tail.sustainSec)

        assertEquals(2000L, loaded.actions.first().delayMs)
    }

    @Test fun `loadRulesV2 returns empty list on blank input`() {
        assertTrue(loadRulesV2("").isEmpty())
    }

    @Test fun `loadRulesV2 returns empty list on malformed JSON`() {
        assertTrue(loadRulesV2("not valid json").isEmpty())
    }

    // ── V1 → V2 migration ─────────────────────────────────────────────────

    @Test fun `migrateToV2 wraps V1 rule in a single-leaf AND group`() {
        @Suppress("DEPRECATION")
        val v1 = LinkRule(
            id = "v1id",
            name = "Old Rule",
            metricKey = "battery_level",
            operator = "lt",
            threshold = "15",
            actionType = "notification",
            actionConfig = mapOf("title" to "Migrated"),
            cooldownSec = 20,
        )
        val v2 = migrateToV2(v1)
        assertEquals("v1id", v2.id)
        assertEquals("Old Rule", v2.name)
        assertEquals(20, v2.cooldownSec)
        val group = v2.root as ConditionNode.Group
        assertEquals(LogicOperator.AND, group.logic)
        assertEquals(1, group.children.size)
        val leaf = group.children[0] as ConditionNode.Leaf
        assertEquals("battery_level", leaf.metricKey)
        assertEquals("15", leaf.threshold)
        assertEquals(1, v2.actions.size)
        assertEquals("notification", v2.actions[0].actionType)
        assertEquals("Migrated", v2.actions[0].actionConfig["title"])
    }

    @Test fun `loadRulesV2 falls back to V1 JSON when input matches old schema`() {
        // A V1-style array payload (the format old persisted data uses)
        val v1Json = """
            [{"id":"x","name":"X","enabled":true,"metricKey":"battery_level","operator":"lt",
              "threshold":"15","thresholdHigh":"","actionType":"notification",
              "actionConfig":{"title":"Hi"},"cooldownSec":10,"lastTriggeredMs":0}]
        """.trimIndent()
        val rules = loadRulesV2(v1Json)
        assertEquals(1, rules.size)
        assertEquals("x", rules[0].id)
        val leaf = (rules[0].root as ConditionNode.Group).children[0] as ConditionNode.Leaf
        assertEquals("battery_level", leaf.metricKey)
        assertEquals("15", leaf.threshold)
    }
}
