package dev.ranzlappen.gadget.core.automation

/**
 * Declarative metadata for one invocable action a module exposes to the
 * automation engine (and any future "run this now" UI). The engine reads
 * these to build its action picker; it never hardcodes a per-feature list
 * — the deliberate fix for the legacy `Link` module's hardcoded
 * `LinkActionType` enum.
 *
 * Not `@Immutable` — `:core:automation` intentionally doesn't depend on
 * Compose. Compose treats data classes of stable members as stable.
 */
data class ModuleAction(
    val key: String,
    val label: String,
    val requiresRoot: Boolean = false,
    val params: List<ActionParam> = emptyList(),
)

/** A single parameter an action accepts, for the rule-builder UI to render. */
data class ActionParam(
    val name: String,
    val type: ActionParamType,
    val default: String = "",
    val min: Float? = null,
    val max: Float? = null,
)

enum class ActionParamType { Int, Float, Text, Bool }

/** Outcome of dispatching an action. */
sealed interface ActionResult {
    data object Success : ActionResult
    data class Failure(val reason: String) : ActionResult
    data object Unsupported : ActionResult
}
