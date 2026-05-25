package dev.ranzlappen.gadget.feature.torch

import androidx.compose.runtime.Immutable

/**
 * Modular seam for the **rooted-flavor** Torch capabilities. Following the
 * repo's root convention (CLAUDE.md rule 3), the feature module never
 * branches on `BuildConfig.IS_ROOTED`; it consults this interface, which
 * Hilt binds to a no-op in the standard flavor and to a real
 * libsu/sysfs-backed implementation (reusing the legacy rooted Torch
 * controller) in the rooted flavor. The standard binding reports
 * everything unavailable so the shared UI uses one code path.
 *
 * Every privileged action routes through the rooted implementation's
 * `RootSafetyGate` (capability + user opt-out + rate limit); there is no
 * fast path that bypasses it.
 */
interface TorchRootCapabilities {

    /** Whether this build is the rooted flavor (true even before [probe]). */
    val isRootedFlavor: Boolean

    /** Cached "is there a usable root shell right now?". False until
     *  [probe] has resolved once. */
    fun hasRootAccess(): Boolean

    /** One-shot probe of root access + supported flashlight sysfs hardware.
     *  Cheap to call repeatedly — the implementation caches. */
    suspend fun probe(): TorchRootAvailability

    /** Drive the LED brightness sysfs node past the stock cap (percent of
     *  `max_brightness`, hard-ceilinged by the implementation). */
    suspend fun boostBrightness(percent: Int): TorchRootResult

    /** Strobe with independent on/off durations for high-frequency,
     *  low-duty pulses. */
    suspend fun dutyCycleStrobe(frequencyHz: Int, dutyPercent: Int, durationMillis: Long): TorchRootResult

    /** Light every detected emitter at once (back/front/notification LEDs,
     *  optionally screen). */
    suspend fun multiLedActivate(durationMillis: Long, includeScreen: Boolean): TorchRootResult

    /** Run a duty-cycle strobe with OS thermal throttling suspended for the
     *  flashlight thermal zone (hard ~45s ceiling enforced downstream). */
    suspend fun thermalOverrideStrobe(frequencyHz: Int, dutyPercent: Int, durationMillis: Long): TorchRootResult
}

/**
 * Resolved availability of the rooted Torch capabilities on this build +
 * device, used to drive the per-function status badges and to show/hide
 * the in-app root controls.
 */
@Immutable
data class TorchRootAvailability(
    val rootedFlavor: Boolean = false,
    val rootAccess: Boolean = false,
    val ledNodeFound: Boolean = false,
) {
    /** Root usable on a rooted build — strobe / multi-LED / thermal paths. */
    val rootReady: Boolean get() = rootedFlavor && rootAccess

    /** Brightness boost additionally needs a recognised LED sysfs node. */
    val brightnessReady: Boolean get() = rootReady && ledNodeFound

    companion object {
        /** Standard-flavor / unprobed default — nothing available. */
        val Unavailable = TorchRootAvailability()
    }
}

/** Outcome of a rooted Torch action. Mirrors the legacy controller's
 *  result tiers so the adapter is a 1:1 mapping. */
sealed interface TorchRootResult {
    /** Completed successfully. */
    data object Ok : TorchRootResult

    /** Standard flavor, missing root, or no matching sysfs node. */
    data object Unsupported : TorchRootResult

    /** The user disabled this capability in settings. */
    data object OptedOut : TorchRootResult

    /** The soft limiter throttled the call; retry after [retryAfterMillis]. */
    data class RateLimited(val retryAfterMillis: Long) : TorchRootResult

    /** A sysfs write / shell exec failed; [message] is human-readable. */
    data class Error(val message: String) : TorchRootResult
}
