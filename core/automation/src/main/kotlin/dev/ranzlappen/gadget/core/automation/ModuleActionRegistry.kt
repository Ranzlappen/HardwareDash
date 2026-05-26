package dev.ranzlappen.gadget.core.automation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The automation engine's entry point to every module's actions. Injects
 * the Hilt-contributed `Map<String, ActionHandler>` (keyed by featureId)
 * so the engine can enumerate available actions and dispatch one without
 * referencing any feature module.
 */
@Singleton
class ModuleActionRegistry @Inject constructor(
    private val handlers: Map<String, @JvmSuppressWildcards ActionHandler>,
) {
    /** Every registered feature handler. */
    fun handlers(): List<ActionHandler> = handlers.values.toList()

    /** Flattened (featureId, action) pairs — what a rule builder lists. */
    fun actions(): List<Pair<String, ModuleAction>> =
        handlers.flatMap { (featureId, handler) -> handler.actions.map { featureId to it } }

    /** Dispatch one action; [ActionResult.Unsupported] if the feature/key is unknown. */
    suspend fun dispatch(
        featureId: String,
        actionKey: String,
        params: Map<String, String> = emptyMap(),
    ): ActionResult = handlers[featureId]?.dispatch(actionKey, params) ?: ActionResult.Unsupported
}
