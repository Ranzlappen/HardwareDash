package dev.ranzlappen.gadget.core.automation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format regression tests for the persisted rule model.
 *
 * Critical invariant: every `@Serializable sealed` subtype under
 * [Trigger] / [Condition] encodes its polymorphic discriminator as the
 * **pinned** `@SerialName` FQN. `automation.db` stores the sealed graphs
 * as JSON columns, so a drifted discriminator silently corrupts every
 * existing user's rules.
 *
 * If these tests turn red, a `@SerialName` pin has been removed or edited.
 * **Do not "fix" the test** — restore the pin instead, or pair the change
 * with a migrator that rewrites the discriminator on read (the
 * `:core:widgetkit` `Migrator<T>` precedent).
 */
class RuleSerializationTest {

    private val json = AutomationJson

    // ─── helpers ────────────────────────────────────────────────────

    private fun ruleWith(trigger: Trigger) = Rule(
        id = "11111111-2222-3333-4444-555555555555",
        name = "test rule",
        enabled = true,
        trigger = trigger,
        conditions = listOf(
            Condition.MetricCompare(metricKey = "battery_level", op = ComparisonOp.Gt, value = 20f),
            Condition.TimeWindow(startMinutes = 22 * 60, endMinutes = 6 * 60),
        ),
        conditionLogic = ConditionLogic.Any,
        actions = listOf(
            RuleAction(
                featureId = "torch",
                actionKey = "off",
                params = mapOf("strength" to "50"),
                requiresRoot = false,
            ),
        ),
        cooldownSeconds = 30,
    )

    private fun roundTrip(rule: Rule): Rule =
        json.decodeFromString(Rule.serializer(), json.encodeToString(Rule.serializer(), rule))

    // ─── round-trips (one per trigger kind) ─────────────────────────

    @Test
    fun metricThresholdRule_roundTrips() {
        val rule = ruleWith(
            Trigger.MetricThreshold(
                metricKey = "proximity",
                op = ComparisonOp.Lt,
                value = 5f,
                edge = Edge.Rising,
                clearValue = 8f,
            ),
        )
        assertEquals(rule, roundTrip(rule))
    }

    @Test
    fun scheduleRule_roundTrips() {
        val rule = ruleWith(
            Trigger.Schedule(
                timeOfDayMinutes = 9 * 60,
                daysOfWeek = setOf(DayOfWeek.Monday, DayOfWeek.Sunday),
                exact = true,
            ),
        )
        assertEquals(rule, roundTrip(rule))
    }

    @Test
    fun systemEventRule_roundTrips() {
        val rule = ruleWith(Trigger.SystemEvent(SystemEventKind.PowerConnected))
        assertEquals(rule, roundTrip(rule))
    }

    @Test
    fun manualRule_roundTrips() {
        val rule = ruleWith(Trigger.Manual)
        assertEquals(rule, roundTrip(rule))
    }

    // ─── discriminator pins (the sacred wire strings) ───────────────

    @Test
    fun metricThresholdPinsDiscriminator() {
        val encoded = json.encodeToString(
            Trigger.serializer(),
            Trigger.MetricThreshold(metricKey = "proximity", op = ComparisonOp.Lt, value = 5f),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Trigger.MetricThreshold\""),
        )
    }

    @Test
    fun schedulePinsDiscriminator() {
        val encoded = json.encodeToString(
            Trigger.serializer(),
            Trigger.Schedule(timeOfDayMinutes = 540),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Trigger.Schedule\""),
        )
    }

    @Test
    fun systemEventPinsDiscriminator() {
        val encoded = json.encodeToString(
            Trigger.serializer(),
            Trigger.SystemEvent(SystemEventKind.BootCompleted),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Trigger.SystemEvent\""),
        )
    }

    @Test
    fun manualPinsDiscriminator() {
        val encoded = json.encodeToString(Trigger.serializer(), Trigger.Manual)
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Trigger.Manual\""),
        )
    }

    @Test
    fun metricComparePinsDiscriminator() {
        val encoded = json.encodeToString(
            Condition.serializer(),
            Condition.MetricCompare(metricKey = "battery_level", op = ComparisonOp.Gte, value = 80f),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Condition.MetricCompare\""),
        )
    }

    @Test
    fun timeWindowPinsDiscriminator() {
        val encoded = json.encodeToString(
            Condition.serializer(),
            Condition.TimeWindow(startMinutes = 0, endMinutes = 360),
        )
        assertTrue(
            "encoded=$encoded must carry the pinned FQN discriminator",
            encoded.contains("\"dev.ranzlappen.gadget.core.automation.Condition.TimeWindow\""),
        )
    }

    // ─── enum wire names (pinned by name, not ordinal) ──────────────

    @Test
    fun enumWireNamesArePinned() {
        val encoded = json.encodeToString(
            Trigger.serializer(),
            Trigger.MetricThreshold(
                metricKey = "m",
                op = ComparisonOp.Lt,
                value = 1f,
                edge = Edge.Falling,
            ),
        )
        assertTrue("op encodes by name: $encoded", encoded.contains("\"Lt\""))
        assertTrue("edge encodes by name: $encoded", encoded.contains("\"Falling\""))

        val schedule = json.encodeToString(
            Trigger.serializer(),
            Trigger.Schedule(timeOfDayMinutes = 0, daysOfWeek = setOf(DayOfWeek.Wednesday)),
        )
        assertTrue("day encodes by name: $schedule", schedule.contains("\"Wednesday\""))
    }

    // ─── forward / backward compatibility ───────────────────────────

    @Test
    fun decodeWithoutOptionalFields_appliesDefaults() {
        // A record written before cooldownSeconds / clearValue / exact /
        // conditionLogic existed (or by a version with encodeDefaults=false,
        // i.e. every record AutomationJson itself writes).
        val legacy = """
            {
              "id": "abc",
              "name": "legacy",
              "trigger": {
                "type": "dev.ranzlappen.gadget.core.automation.Trigger.MetricThreshold",
                "metricKey": "proximity",
                "op": "Lt",
                "value": 5.0
              }
            }
        """.trimIndent()
        val decoded = json.decodeFromString(Rule.serializer(), legacy)
        assertEquals(true, decoded.enabled)
        assertEquals(emptyList<Condition>(), decoded.conditions)
        assertEquals(ConditionLogic.All, decoded.conditionLogic)
        assertEquals(emptyList<RuleAction>(), decoded.actions)
        assertEquals(0, decoded.cooldownSeconds)
        val trigger = decoded.trigger as Trigger.MetricThreshold
        assertEquals(Edge.Rising, trigger.edge)
        assertEquals(null, trigger.clearValue)
    }

    @Test
    fun decodeScheduleWithoutOptionalFields_appliesDefaults() {
        val legacy = """
            {
              "type": "dev.ranzlappen.gadget.core.automation.Trigger.Schedule",
              "timeOfDayMinutes": 540
            }
        """.trimIndent()
        val decoded = json.decodeFromString(Trigger.serializer(), legacy) as Trigger.Schedule
        assertEquals(DayOfWeek.everyDay(), decoded.daysOfWeek)
        assertEquals(false, decoded.exact)
    }

    @Test
    fun unknownKeysAreIgnored() {
        // A record written by a NEWER app version carrying a field this
        // version doesn't know must still decode (ignoreUnknownKeys).
        val future = """
            {
              "id": "abc",
              "name": "future",
              "someFutureField": "ignored",
              "trigger": {
                "type": "dev.ranzlappen.gadget.core.automation.Trigger.Manual"
              }
            }
        """.trimIndent()
        val decoded = json.decodeFromString(Rule.serializer(), future)
        assertEquals(Trigger.Manual, decoded.trigger)
    }
}
