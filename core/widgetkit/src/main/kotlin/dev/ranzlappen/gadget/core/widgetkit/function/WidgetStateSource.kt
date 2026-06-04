package dev.ranzlappen.gadget.core.widgetkit.function

/**
 * Live on/off state for a [WidgetFunctionBehavior.Toggle] — read by the
 * generic provider to drive the active/inactive icon swap and to decide which
 * paired action a tap dispatches.
 *
 * A feature binds one per toggleable signal into a
 * `Map<String, WidgetStateSource>` multibinding keyed `"<featureId>:<stateKey>"`
 * (e.g. `"torch:torch_power"` → `TorchController.state.value.isOn`). Kept
 * **non-suspend** — implementations read a hot `StateFlow.value`, so the
 * provider can compute the pre-tap state cheaply on the broadcast path.
 *
 * Momentary functions need no state source; an absent key resolves to
 * inactive.
 */
fun interface WidgetStateSource {
    fun isActive(): Boolean
}
