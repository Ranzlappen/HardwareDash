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

    /**
     * Authoritative active state used to decide a **toggle tap's** direction,
     * awaited on the (possibly cold) widget broadcast process. Defaults to the
     * synchronous [isActive]; a source whose backing state is populated
     * **asynchronously** — e.g. the torch reads its real on/off from
     * `CameraManager.TorchCallback`, delivered on the main thread shortly after
     * registration — overrides this to await its first authoritative delivery.
     *
     * Without it, a freshly-spawned widget process reads the source's stale
     * *initial* value (torch: `false`) and so always dispatches the "on"
     * action — the "toggle on works, off doesn't" bug. Implementations must
     * keep the wait **bounded** so it can never stall the broadcast.
     */
    suspend fun awaitActive(): Boolean = isActive()
}
