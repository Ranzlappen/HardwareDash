package dev.ranzlappen.gadget.core.automation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A state gate re-checked at the moment a [Trigger] fires. Conditions never
 * wake a rule on their own; they only permit/deny the actions once the
 * trigger has fired — that keeps evaluation event-driven instead of a
 * continuous re-scan. The list on [Rule.conditions] folds to one boolean
 * via [Rule.conditionLogic] (flat ALL/ANY — deliberately no nested boolean
 * tree in v1; a `Group` subtype can join this sealed family later).
 *
 * **Wire format is sacred** — pinned [SerialName]s, same contract as
 * [Trigger]; see [RuleSerializationTest].
 */
@Serializable
sealed interface Condition {

    /** Compare the current value of a `MetricSource` reading. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Condition.MetricCompare")
    data class MetricCompare(
        val metricKey: String,
        val op: ComparisonOp,
        val value: Float,
    ) : Condition

    /**
     * Local-time window, minutes after midnight (`0..1439`). A window where
     * `endMinutes < startMinutes` wraps midnight (22:00–06:00).
     */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Condition.TimeWindow")
    data class TimeWindow(
        val startMinutes: Int,
        val endMinutes: Int,
    ) : Condition

    /**
     * A nested boolean group — the deferred `Group` node the flat v1 sealed
     * shape reserved room for. Its [children] fold to one boolean via the
     * group's own [logic] (ALL/ANY), so a rule can express
     * `A AND (B OR C) AND …` without a flat single-level fold. Groups nest
     * arbitrarily deep; the evaluator recurses.
     *
     * An empty group is vacuously **true** for both logics (matching the
     * top-level [Rule.conditions] empty semantics), so a half-built group in
     * the editor never silently blocks a rule.
     */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Condition.Group")
    data class Group(
        val logic: ConditionLogic = ConditionLogic.All,
        val children: List<Condition> = emptyList(),
    ) : Condition
}
