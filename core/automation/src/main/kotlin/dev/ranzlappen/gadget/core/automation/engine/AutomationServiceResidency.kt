package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger

/**
 * The pure residency predicate for `AutomationService` (ADR-0002 Decision 4
 * / `docs/automation-engine.md` § Runtime host): the foreground service is
 * resident **only while ≥1 enabled rule needs a live in-process
 * subscription** — a [Trigger.MetricThreshold] (continuous `MetricSource`
 * stream) or a [SystemEventKind.Connectivity] system event (a registered
 * `ConnectivityManager.NetworkCallback`; connectivity broadcasts stopped
 * being deliverable to manifest receivers in Android N).
 *
 * Schedule-, power-event-, boot-, and manual-triggered rules evaluate
 * **one-shot** (alarm / broadcast / tap → start, evaluate, dispatch, stop)
 * with no resident service or ongoing notification, so a user whose rules
 * are all "at 09:00…" / "on power connected…" never sees an automation
 * notification.
 *
 * Pure so the residency rule is JVM-tested away from the Android service
 * lifecycle; the service calls [isServiceRequired] on every rule-set change
 * and self-stops when it returns false (the `MonitorService` self-stop
 * pattern). The boot re-arm handler and the rule-builder's save path call
 * the same predicate ([requiresResidency] for a single rule) so all three
 * agree on when the service must run.
 */
object AutomationServiceResidency {

    /** True iff [rule] alone would keep the service resident. */
    fun requiresResidency(rule: Rule): Boolean = rule.enabled &&
        (
            rule.trigger is Trigger.MetricThreshold ||
                rule.trigger == Trigger.SystemEvent(SystemEventKind.Connectivity)
            )

    /** True iff some enabled rule needs a live in-process subscription. */
    fun isServiceRequired(rules: List<Rule>): Boolean = rules.any(::requiresResidency)

    /** The enabled metric-stream rules the resident service must subscribe to. */
    fun streamingRules(rules: List<Rule>): List<Rule> =
        rules.filter { it.enabled && it.trigger is Trigger.MetricThreshold }

    /** The enabled rules wired to the resident network callback. */
    fun connectivityRules(rules: List<Rule>): List<Rule> =
        rules.filter { it.enabled && it.trigger == Trigger.SystemEvent(SystemEventKind.Connectivity) }
}
