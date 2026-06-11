package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger

/**
 * The pure residency predicate for `AutomationService` (ADR-0002 Decision 4
 * / `docs/automation-engine.md` § Runtime host): the foreground service is
 * resident **only while ≥1 enabled rule has a metric-stream trigger** —
 * `Trigger.MetricThreshold`, the one trigger kind that needs a continuous
 * `MetricSource` subscription.
 *
 * Schedule-, system-event-, and manual-triggered rules evaluate **one-shot**
 * (alarm / broadcast / tap → start, evaluate, dispatch, stop) with no
 * resident service or ongoing notification, so a user whose rules are all
 * "at 09:00…" / "on power connected…" never sees an automation notification.
 *
 * Pure so the residency rule is JVM-tested away from the Android service
 * lifecycle; the service calls [isServiceRequired] on every rule-set change
 * and self-stops when it returns false (the `MonitorService` self-stop
 * pattern).
 */
object AutomationServiceResidency {

    /** True iff some enabled rule is driven by a continuous metric stream. */
    fun isServiceRequired(rules: List<Rule>): Boolean =
        rules.any { it.enabled && it.trigger is Trigger.MetricThreshold }

    /** The enabled metric-stream rules the resident service must subscribe to. */
    fun streamingRules(rules: List<Rule>): List<Rule> =
        rules.filter { it.enabled && it.trigger is Trigger.MetricThreshold }
}
