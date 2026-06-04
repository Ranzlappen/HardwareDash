package dev.ranzlappen.gadget.core.widgetkit.function

import dev.ranzlappen.gadget.core.automation.ActionParam

/**
 * One user-selectable widget **function** — the unit a generic widget binds
 * to. A function names a stable [id] (persisted in the widget config's
 * `actionKey`), a user-facing [label], a [params] schema (reused verbatim
 * from `:core:automation`'s [ActionParam] so the dialog can auto-generate
 * editors), and a [behavior] that decides how a tap is dispatched and
 * whether the widget shows live on/off state.
 *
 * This is a **widget-side** concept, deliberately not part of the
 * `:core:automation` contract: a [WidgetFunctionBehavior.Toggle] pairs two
 * existing automation actions (on/off, start/stop) into a single tap surface
 * with live state — something only a widget needs. The automation engine
 * keeps the two actions independent.
 *
 * Features build their `List<WidgetFunction>` in a per-feature
 * `WidgetFunctionCatalog` from their `ActionHandler.actions` plus any
 * hand-authored toggle pairings. The generic dialog filters the list by the
 * current flavor (a `requiresRoot` function is hidden when root is
 * unavailable) and the generic provider dispatches the selected one through
 * [WidgetFunctionDispatcher].
 */
data class WidgetFunction(
    /** Stable id persisted in the widget config's `actionKey`. For a
     *  momentary function this is conventionally the backing action key; for
     *  a toggle it is a distinct composite id (e.g. `"torch_power"`) so it
     *  never collides with the raw on/off action keys it pairs. */
    val id: String,
    val label: String,
    val requiresRoot: Boolean = false,
    val params: List<ActionParam> = emptyList(),
    val behavior: WidgetFunctionBehavior,
)

/**
 * How a [WidgetFunction] dispatches a tap and renders state.
 *
 * - [Momentary] — one tap fires [actionKey] once. No persistent state, so the
 *   widget shows its resting icon + a press frame and reports "triggered".
 * - [Toggle] — one tap flips between [onActionKey] / [offActionKey] based on a
 *   live [stateKey]-selected [WidgetStateSource]; drives the active/inactive
 *   icon swap and reports "on"/"off".
 */
sealed interface WidgetFunctionBehavior {

    data class Momentary(val actionKey: String) : WidgetFunctionBehavior

    data class Toggle(
        val onActionKey: String,
        val offActionKey: String,
        /** Selects the feature's `WidgetStateSource` keyed
         *  `"<featureId>:<stateKey>"`. */
        val stateKey: String,
    ) : WidgetFunctionBehavior
}
