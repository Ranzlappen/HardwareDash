package dev.ranzlappen.gadget.feature.torch

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the device's torch / flashlight.
 *
 * One `@Singleton` instance lives in the Hilt graph. All four
 * entry points (TorchScreen, Quick-Settings tile, on/off home
 * widget, strobe service) converge on the **same** controller so
 * a toggle from one surface is reflected immediately in every
 * other surface that's observing [state].
 *
 * Phase 2 / Batch 1 ships [StandardTorchController] backed by
 * Camera2's `CameraManager.setTorchMode(...)`. The rooted-flavor
 * implementation will land as a sibling `:feature:torch-rooted`
 * module (mirroring `:feature:lock-rooted` etc.) and supersede
 * the binding via `rootedImplementation` — tracked at
 * https://github.com/Ranzlappen/HardwareDash/issues/94. Call
 * sites here remain unchanged either way.
 */
interface TorchController {

    /**
     * Reactive snapshot of the torch's current state. Hot — emits
     * a fresh [TorchState] every time the underlying hardware /
     * permission / availability signal changes, including changes
     * triggered by other apps (the OS-wide torch callback).
     */
    val state: StateFlow<TorchState>

    /**
     * Flip the torch's `isOn` boolean. No-op if not available.
     *
     * Synchronous because Camera2's `setTorchMode` is a fast
     * binder call. Callers don't need to wrap in a coroutine.
     */
    fun toggle()

    /** Set the torch to a specific on/off state. No-op if not available. */
    fun setOn(on: Boolean)
}

/**
 * Plain-data snapshot of torch state. Marked [Immutable] so
 * Compose can skip recompositions when the value is structurally
 * unchanged.
 *
 * - [isOn] — current on/off, reflective of the underlying hardware
 *   (NOT just the last command sent — the [TorchController]
 *   listens to the OS's torch callback so external changes
 *   (notification panel tile, etc.) also flip this).
 * - [isAvailable] — `false` when the device has no flash unit
 *   (e.g. emulators, flashless tablets). UI should disable
 *   torch-related controls and show an "Unavailable" state.
 * - [error] — last error encountered, or `null` if no error.
 *   `null` after a successful operation (errors aren't sticky).
 */
@Immutable
data class TorchState(
    val isOn: Boolean = false,
    val isAvailable: Boolean = false,
    val error: TorchError? = null,
)

/** Discrete error categories. Specific enough for the UI to react. */
@Immutable
enum class TorchError {
    /** No flash unit on this device. */
    NoFlashUnit,

    /** Camera2 reported a hardware failure when toggling. */
    HardwareError,

    /** CAMERA permission not granted (rare for torch-only on most OEMs). */
    PermissionDenied,
}
