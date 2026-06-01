package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Modular seam for the **rooted-flavor** Vibration capabilities — the
 * extreme-tier surface that bypasses `android.os.Vibrator` and writes the PWM
 * duty cycle directly to the motor sysfs node.
 *
 * Following the repo's root convention (CLAUDE.md rule 3), the feature module
 * never branches on `BuildConfig.IS_ROOTED`; it consults this interface, which
 * Hilt binds to a no-op in the standard flavor
 * ([dev.ranzlappen.gadget.feature.vibration.standard.StandardVibrationRootCapabilities])
 * and to a real libsu/sysfs-backed impl in the rooted flavor
 * (`:feature:vibration-rooted`, reusing the legacy rooted controller). The
 * standard binding reports everything unavailable so the shared UI uses one
 * code path.
 *
 * Every privileged action routes through the rooted implementation's
 * `RootSafetyGate` (capability + user opt-out + rate limit) keyed by a
 * `RootFeatureKey.Vibration*`; there is no fast path that bypasses it.
 */
interface VibrationRootCapabilities {

    /** Whether this build is the rooted flavor (true even before [probe]). */
    val isRootedFlavor: Boolean

    /**
     * The motor's full-scale amplitude ceiling as a **live** percent. A
     * constant `100` on standard. On the rooted flavor it can rise above 100
     * once [probe] confirms a writable sysfs node that accepts over-drive — so
     * the monitoring chart's y-axis scales to the real range. Folded into the
     * metric descriptor's `maxFlow`.
     */
    val maxAmplitudePercentFlow: StateFlow<Int>

    /**
     * Live last-commanded amplitude as a percent of the stock max: `0` while
     * idle, up to the ceiling reported by [maxAmplitudePercentFlow] during an
     * extreme-tier command. Standard reports a constant `0`. Drives the
     * monitoring metric's live value on the rooted flavor.
     */
    val commandedAmplitudePercent: StateFlow<Int>

    /** Cached "is there a usable root shell right now?". False until [probe]
     *  has resolved once. */
    fun hasRootAccess(): Boolean

    /** One-shot probe of root access + supported vibration sysfs hardware.
     *  Cheap to call repeatedly — the implementation caches. */
    suspend fun probe(): VibrationRootAvailability

    /**
     * Drive the primary motor at [amplitudePercent] (0–100, may exceed the
     * API's clamp) for [durationMillis]. Hard-capped at a 3 s burst by the
     * implementation.
     */
    suspend fun extremeAmplitude(amplitudePercent: Int, durationMillis: Long): VibrationRootResult

    /**
     * Play an arbitrary PWM micro-pattern. The implementation enforces a
     * minimum 5 ms off-time per pulse.
     */
    suspend fun directPwm(pattern: List<PwmPulse>): VibrationRootResult

    /**
     * Drive LRA + ERM motors with independent patterns aligned at
     * [phaseOffsetMicros]. Per-actuator amplitude clamped at 80 % inside the
     * driver. Unsupported when the device lacks dual actuators.
     */
    suspend fun dualActuator(
        lraPattern: List<PwmPulse>,
        ermPattern: List<PwmPulse>,
        phaseOffsetMicros: Long,
    ): VibrationRootResult

    /**
     * Continuous rumble for [durationMillis] at [amplitudePercent], with
     * battery-drain + motor-temp monitoring that aborts early on breach.
     * Hard-capped at 5 minutes.
     */
    suspend fun sustainedRumble(durationMillis: Long, amplitudePercent: Int): VibrationRootResult
}

/**
 * One on/off pulse for a raw-PWM pattern. Microsecond resolution where the
 * underlying shell `usleep` supports it; the wave generator enforces a minimum
 * 5 ms off-time floor regardless of input. Lives in the feature module so both
 * the interface and the rooted impl reference one type.
 */
@Immutable
data class PwmPulse(val onMicros: Long, val offMicros: Long)

/**
 * Resolved availability of the rooted Vibration capabilities on this build +
 * device, used to drive the per-function status badges and to show/hide the
 * in-app root controls.
 */
@Immutable
data class VibrationRootAvailability(
    val rootedFlavor: Boolean = false,
    val rootAccess: Boolean = false,
    val nodeFound: Boolean = false,
    val hasDualActuators: Boolean = false,
) {
    /** Root usable on a rooted build with a writable motor node — amplitude /
     *  PWM / rumble paths. */
    val rootReady: Boolean get() = rootedFlavor && rootAccess && nodeFound

    /** Dual-actuator additionally needs both LRA + ERM nodes present. */
    val dualReady: Boolean get() = rootReady && hasDualActuators

    companion object {
        /** Standard-flavor / unprobed default — nothing available. */
        val Unavailable = VibrationRootAvailability()
    }
}

/** Outcome of a rooted Vibration action. Mirrors the legacy controller's
 *  result tiers so the adapter is a 1:1 mapping. */
sealed interface VibrationRootResult {
    /** Completed successfully. */
    data object Ok : VibrationRootResult

    /** Standard flavor, missing root, or no matching sysfs node. */
    data object Unsupported : VibrationRootResult

    /** The user disabled this capability in settings. */
    data object OptedOut : VibrationRootResult

    /** The soft limiter throttled the call; retry after [retryAfterMillis]. */
    data class RateLimited(val retryAfterMillis: Long) : VibrationRootResult

    /** A sysfs write / shell exec failed; [message] is human-readable. */
    data class Error(val message: String) : VibrationRootResult
}
