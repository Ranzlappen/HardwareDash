package dev.ranzlappen.gadget.core.automation

/**
 * A feature's invocable-action surface for automation.
 *
 * Each feature binds one handler into a Hilt `Map<String, ActionHandler>`
 * (`@IntoMap @StringKey(featureId)`). It declares its [actions] (metadata)
 * and dispatches them by key. The automation engine resolves a handler
 * from the registry and calls [dispatch] — it never imports the feature.
 *
 * `TorchActionHandler` is the reference implementation.
 */
interface ActionHandler {

    /** Stable id for this feature (the registry map key), e.g. `"torch"`. */
    val featureId: String

    /** The actions this feature exposes. */
    val actions: List<ModuleAction>

    /**
     * Run [actionKey] with [params] (raw string values keyed by
     * [ActionParam.name]; the handler parses/validates). Returns
     * [ActionResult.Unsupported] for an unknown key.
     */
    suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult
}
