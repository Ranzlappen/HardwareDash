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

    /**
     * Set the flash-unit intensity. Level is normalised `0f..1f` where
     * `1f` is maximum brightness.
     *
     * On API 33+ devices with `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1`
     * this calls `CameraManager.turnOnTorchWithStrengthLevel` (which
     * also turns the torch on if it is currently off). On unsupported
     * devices this updates [state] with [TorchError.BrightnessUnsupported]
     * and is otherwise a no-op.
     */
    fun setBrightness(level: Float)

    /**
     * The torch's **authoritative** current state, awaiting the first real
     * hardware delivery if it hasn't arrived yet.
     *
     * [state] is seeded `isOn = false` and only corrected once the OS torch
     * callback fires (asynchronously, on the main thread). A synchronous
     * `state.value` read on a freshly-spawned process — e.g. a home-screen
     * widget tap, whose broadcast can run in a brand-new process — can therefore
     * still see the stale initial `false`. Callers that must branch on the
     * *real* state (deciding a toggle's direction) await this instead.
     * Implementations bound the wait so it can never stall a broadcast; the
     * default returns the cached [state] value for impls whose state is
     * synchronously accurate.
     */
    suspend fun currentState(): TorchState = state.value
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
 * - [brightness] — normalised intensity in `0f..1f`; only meaningful
 *   when [isOn]. `1f` represents maximum (hardware default). Updated
 *   by [TorchController.setBrightness].
 * - [brightnessSupported] — `true` when the device exposes variable
 *   flash intensity (API 33+ and `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1`).
 *   When `false`, [TorchController.setBrightness] is a no-op.
 * - [error] — last error encountered, or `null` if no error.
 *   `null` after a successful operation (errors aren't sticky).
 */
@Immutable
data class TorchState(
    val isOn: Boolean = false,
    val isAvailable: Boolean = false,
    val brightness: Float = 1f,
    val brightnessSupported: Boolean = false,
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

    /** [TorchController.setBrightness] was called but the device doesn't
     *  support variable torch intensity (API < 33 or single-level flash). */
    BrightnessUnsupported,
}
