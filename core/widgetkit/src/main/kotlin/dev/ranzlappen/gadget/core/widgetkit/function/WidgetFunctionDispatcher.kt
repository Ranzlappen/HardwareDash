package dev.ranzlappen.gadget.core.widgetkit.function

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleActionRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes a widget tap to the right `:core:automation` action(s) and reports
 * the post-tap state for the icon swap + feedback. One app-wide `@Singleton`
 * serving every widget-bearing feature.
 *
 * - [isActive] — synchronous pre-tap state for a [WidgetFunctionBehavior.Toggle]
 *   (reads the bound [WidgetStateSource]); always `false` for a momentary
 *   function. The provider reads it to paint the resting icon.
 * - [dispatch] — runs the function: a toggle reads the live state, dispatches
 *   the on-or-off action, and returns the flipped state on success; a
 *   momentary function dispatches once and returns `active = false`.
 *
 * It owns no hardware — it delegates to [ModuleActionRegistry], so the kit
 * never imports a feature. `:core:widgetkit` depends on `:core:automation`
 * (not vice-versa).
 */
@Singleton
class WidgetFunctionDispatcher @Inject constructor(
    private val registry: ModuleActionRegistry,
    private val stateSources: Map<String, @JvmSuppressWildcards WidgetStateSource>,
) {

    /** Pre-tap active state used to paint the resting icon. */
    fun isActive(featureId: String, function: WidgetFunction): Boolean =
        when (val behavior = function.behavior) {
            is WidgetFunctionBehavior.Toggle -> stateActive(featureId, behavior.stateKey)
            is WidgetFunctionBehavior.Momentary -> false
        }

    /**
     * Dispatch [function] with [params] (raw string values keyed by
     * [dev.ranzlappen.gadget.core.automation.ActionParam.name]). Returns the
     * post-tap active state (for the icon swap) + the [ActionResult] (for
     * feedback).
     */
    suspend fun dispatch(
        featureId: String,
        function: WidgetFunction,
        params: Map<String, String>,
    ): WidgetDispatchOutcome =
        when (val behavior = function.behavior) {
            is WidgetFunctionBehavior.Toggle -> {
                val wasActive = stateActive(featureId, behavior.stateKey)
                val actionKey = if (wasActive) behavior.offActionKey else behavior.onActionKey
                val result = registry.dispatch(featureId, actionKey, params)
                // Only commit the flipped state when the action succeeded;
                // otherwise the icon stays on the actual hardware state.
                val active = if (result is ActionResult.Success) !wasActive else wasActive
                WidgetDispatchOutcome(active = active, result = result)
            }
            is WidgetFunctionBehavior.Momentary ->
                WidgetDispatchOutcome(
                    active = false,
                    result = registry.dispatch(featureId, behavior.actionKey, params),
                )
        }

    private fun stateActive(featureId: String, stateKey: String): Boolean =
        stateSources["$featureId:$stateKey"]?.isActive() ?: false
}

/** The result of dispatching a widget function: the post-tap active state used
 *  for the icon swap, and the [ActionResult] used for feedback. */
data class WidgetDispatchOutcome(
    val active: Boolean,
    val result: ActionResult,
)
