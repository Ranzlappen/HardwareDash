package dev.ranzlappen.gadget.core.data.automation

import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.ConditionLogic
import dev.ranzlappen.gadget.core.automation.model.DayOfWeek
import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JSON-column round-trip for the entity ↔ model mapping. Pure JVM — the
 * mapper is deliberately Room-free so this runs in the `unit-tests` CI job.
 */
class RuleMapperTest {

    private val rule = Rule(
        id = "11111111-2222-3333-4444-555555555555",
        name = "proximity torch-off",
        enabled = true,
        trigger = Trigger.MetricThreshold(
            metricKey = "proximity",
            op = ComparisonOp.Lt,
            value = 5f,
            edge = Edge.Rising,
            clearValue = 8f,
        ),
        conditions = listOf(
            Condition.MetricCompare("battery_level", ComparisonOp.Gt, 20f),
            Condition.TimeWindow(startMinutes = 22 * 60, endMinutes = 6 * 60),
        ),
        conditionLogic = ConditionLogic.Any,
        actions = listOf(RuleAction("torch", "off", mapOf("k" to "v"), requiresRoot = false)),
        cooldownSeconds = 30,
    )

    @Test
    fun entityRoundTrip_preservesTheRule() {
        val entity = RuleMapper.toEntity(rule, createdAt = 1L, updatedAt = 2L, lastFiredAt = 3L)
        assertEquals(rule, RuleMapper.toModel(entity))
    }

    @Test
    fun entityCarriesTimestamps() {
        val entity = RuleMapper.toEntity(rule, createdAt = 10L, updatedAt = 20L, lastFiredAt = null)
        assertEquals(10L, entity.createdAt)
        assertEquals(20L, entity.updatedAt)
        assertEquals(null, entity.lastFiredAt)
        assertEquals(30, entity.cooldownSeconds)
    }

    @Test
    fun scheduleTrigger_roundTrips() {
        val schedule = rule.copy(
            trigger = Trigger.Schedule(
                timeOfDayMinutes = 540,
                daysOfWeek = setOf(DayOfWeek.Monday),
                exact = true,
            ),
        )
        val entity = RuleMapper.toEntity(schedule, createdAt = 1L, updatedAt = 1L, lastFiredAt = null)
        assertEquals(schedule, RuleMapper.toModel(entity))
    }

    @Test
    fun unknownConditionLogic_fallsBackToAll() {
        val entity = RuleMapper.toEntity(rule, createdAt = 1L, updatedAt = 1L, lastFiredAt = null)
            .copy(conditionLogic = "SomeFutureLogic")
        assertEquals(ConditionLogic.All, RuleMapper.toModel(entity).conditionLogic)
    }
}
