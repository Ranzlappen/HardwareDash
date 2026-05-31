package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the device's standard-tier haptics
 * (`android.os.Vibrator` / `VibratorManager`).
 *
 * One `@Singleton` instance lives in the Hilt graph. The screen, the
 * pattern/one-shot widgets, and [VibrationPlaybackService] converge on the
 * **same** controller so commands from any surface flow through the shared
 * [VibrationRuntime] signal that the monitoring chart polls.
 *
 * This is the standard, non-privileged path — `VibrationEffect`-based
 * one-shots, waveforms, and predefined effects, with amplitude control where
 * the hardware reports `hasAmplitudeControl()`. The privileged extreme-tier
 * surface (direct sysfs PWM, dual-actuator, sustained rumble) is a separate
 * seam — [VibrationRootCapabilities] — bound to a real impl only on the rooted
 * flavor.
 */
interface VibrationController {

    /**
     * Reactive snapshot of the motor's last-commanded state, mirrored into
     * [VibrationRuntime]. Hot — emits a fresh [VibrationState] on every
     * command and when a timed command decays back to idle.
     */
    val state: StateFlow<VibrationState>

    /**
     * Fire a single vibration at [amplitudePercent] (1–100) for
     * [durationMillis]. On devices without amplitude control the percent is
     * ignored and the motor runs at its fixed strength.
     */
    fun oneShot(amplitudePercent: Int, durationMillis: Long)

    /**
     * Play a waveform: alternating off/on segments from [timingsMillis] with
     * matching per-segment strengths in [amplitudes] (1–255, or
     * [AMPLITUDE_DEFAULT] for "device default"). Loops from index 0 when
     * [loop] is true until [stop]; plays once otherwise.
     */
    fun playPattern(timingsMillis: LongArray, amplitudes: IntArray, loop: Boolean)

    /**
     * Play one of the OS's predefined haptic primitives (see
     * [VibrationPredefinedEffect]). No-op on API levels / devices that don't
     * expose the effect.
     */
    fun playPredefined(effect: VibrationPredefinedEffect)

    /** Cancel any in-flight vibration and reset the runtime signal to idle. */
    fun stop()

    companion object {
        /** Sentinel amplitude meaning "use the device's default strength"
         *  (maps to `VibrationEffect.DEFAULT_AMPLITUDE`). */
        const val AMPLITUDE_DEFAULT = -1
    }
}

/**
 * Plain-data snapshot of the standard haptic state. `@Immutable` so Compose
 * skips recompositions when structurally unchanged.
 *
 * - [amplitudePercent] — the last commanded strength as a percent of full
 *   scale; `0` while idle. The monitoring metric polls this.
 * - [isActive] — whether a command is currently playing (a timed command
 *   flips this back to false on expiry; a looping one stays true until [stop]).
 * - [isAvailable] — `false` when the device has no vibrator.
 * - [hasAmplitudeControl] — whether the hardware honours per-segment
 *   amplitude (`Vibrator.hasAmplitudeControl()`); drives the amplitude
 *   slider's enabled state + a `ModuleCapability` row.
 */
@Immutable
data class VibrationState(
    val amplitudePercent: Int = 0,
    val isActive: Boolean = false,
    val isAvailable: Boolean = false,
    val hasAmplitudeControl: Boolean = false,
)

/**
 * The OS predefined haptic primitives surfaced in the UI. Mapped to
 * `VibrationEffect.EFFECT_*` constants (API 29+) by the controller; the names
 * stay stable for persistence + automation params.
 */
enum class VibrationPredefinedEffect {
    Click,
    DoubleClick,
    Tick,
    HeavyClick,
}
