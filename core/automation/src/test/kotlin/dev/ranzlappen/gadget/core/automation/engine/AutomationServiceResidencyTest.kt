package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationServiceResidencyTest {

    private var seq = 0
    private fun rule(trigger: Trigger, enabled: Boolean = true) =
        Rule(id = "r${seq++}", name = "r", enabled = enabled, trigger = trigger)

    private val metric = Trigger.MetricThreshold("proximity", ComparisonOp.Lt, 5f)
    private val schedule = Trigger.Schedule(timeOfDayMinutes = 540)
    private val systemEvent = Trigger.SystemEvent(SystemEventKind.PowerConnected)
    private val manual = Trigger.Manual

    @Test
    fun noRules_serviceNotRequired() {
        assertFalse(AutomationServiceResidency.isServiceRequired(emptyList()))
    }

    @Test
    fun onlyScheduleEventManualRules_serviceNotRequired() {
        val rules = listOf(rule(schedule), rule(systemEvent), rule(manual))
        assertFalse(AutomationServiceResidency.isServiceRequired(rules))
    }

    @Test
    fun anEnabledMetricRule_requiresService() {
        val rules = listOf(rule(schedule), rule(metric))
        assertTrue(AutomationServiceResidency.isServiceRequired(rules))
    }

    @Test
    fun aDisabledMetricRule_doesNotRequireService() {
        val rules = listOf(rule(metric, enabled = false), rule(schedule))
        assertFalse(AutomationServiceResidency.isServiceRequired(rules))
    }

    @Test
    fun streamingRules_returnsOnlyEnabledMetricRules() {
        val enabledMetric = rule(metric)
        val rules = listOf(
            enabledMetric,
            rule(metric, enabled = false),
            rule(schedule),
            rule(manual),
        )
        val streaming = AutomationServiceResidency.streamingRules(rules)
        assertEquals(listOf(enabledMetric), streaming)
    }
}
