package dev.ranzlappen.gadget.core.automation.model

import kotlinx.serialization.Serializable

/**
 * One persisted automation rule:
 * `when <trigger> [if <conditions>] then <actions>`.
 *
 * Stored in `automation.db`'s `rules` table with the sealed graphs
 * ([trigger] / [conditions] / [actions]) as kotlinx-serialization JSON
 * columns (ADR-0002 Decision 5) — which is why every sealed subtype in
 * this package carries a pinned `@SerialName` and a wire-format
 * regression test.
 *
 * [cooldownSeconds] is the per-rule storm bound (ADR-0002 Decision 8): the
 * minimum seconds between firings, `0` = none. The repository persists the
 * last-fired timestamp (`last_fired_at`) so cooldowns survive process death
 * and reboot — a metric flapping across its threshold at restart cannot
 * re-storm.
 */
@Serializable
data class Rule(
    /** Stable unique id (UUID string); generation is the caller's concern. */
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val conditionLogic: ConditionLogic = ConditionLogic.All,
    val actions: List<RuleAction> = emptyList(),
    val cooldownSeconds: Int = 0,
)

/** How [Rule.conditions] folds to one boolean: ALL (AND) or ANY (OR). */
@Serializable
enum class ConditionLogic { All, Any }

/**
 * A reference into the `ActionHandler` registry — dispatched verbatim via
 * `ModuleActionRegistry.dispatch(featureId, actionKey, params)`. The engine
 * never imports a feature (ADR-0002 Decision 2).
 *
 * [requiresRoot] mirrors the `ModuleAction.requiresRoot` flag, cached at
 * author time so the evaluator can drop root actions on a standard build
 * (gating layer 2 — protects rules restored from a rooted device's backup)
 * without consulting the registry.
 */
@Serializable
data class RuleAction(
    val featureId: String,
    val actionKey: String,
    val params: Map<String, String> = emptyMap(),
    val requiresRoot: Boolean = false,
)
