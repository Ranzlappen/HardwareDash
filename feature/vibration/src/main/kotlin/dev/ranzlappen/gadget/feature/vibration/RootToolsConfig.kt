package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * User-tunable parameters for the four rooted Vibration tools, persisted by
 * [RootToolsConfigRepository] so a chosen amplitude / pattern shape survives
 * app restarts. The mirror of torch's `TorchRootToolsConfig` — the screen's
 * [components.VibrationRootToolsCard] edits this record through sliders and the
 * four run buttons dispatch with these values via [VibrationRootCapabilities].
 *
 * The amplitude **maximum** is NOT a constant — it's the live ceiling from
 * [VibrationRootCapabilities.maxAmplitudePercentFlow]. Use [coercedTo] to clamp
 * a candidate record against that live ceiling before persisting / running.
 */
@Serializable
@Immutable
data class VibrationRootToolsConfig(
    /** Extreme-amplitude target, percent of full scale. */
    val extremeAmplitudePercent: Int = DEFAULT_AMPLITUDE_PERCENT,
    /** Extreme-amplitude burst duration (ms), capped at [MAX_BURST_MS]. */
    val extremeBurstMs: Long = DEFAULT_BURST_MS,
    /** Direct-PWM per-pulse on-time (µs). */
    val pwmOnMicros: Long = DEFAULT_PWM_ON_MICROS,
    /** Direct-PWM per-pulse off-time (µs); floored at [MIN_PWM_OFF_MICROS]. */
    val pwmOffMicros: Long = DEFAULT_PWM_OFF_MICROS,
    /** Direct-PWM pulse count. */
    val pwmPulses: Int = DEFAULT_PWM_PULSES,
    /** Dual-actuator LRA↔ERM phase offset (µs). */
    val dualPhaseOffsetMicros: Long = DEFAULT_DUAL_PHASE_MICROS,
    /** Sustained-rumble duration (ms), capped at [MAX_RUMBLE_MS]. */
    val rumbleDurationMs: Long = DEFAULT_RUMBLE_MS,
    /** Sustained-rumble amplitude, percent of full scale. */
    val rumbleAmplitudePercent: Int = DEFAULT_RUMBLE_AMPLITUDE_PERCENT,
) {
    /**
     * Return a copy clamped to the device's real limits: amplitudes into
     * `[MIN_AMPLITUDE_PERCENT, maxAmplitudePercent]` (the live ceiling),
     * durations to their caps, PWM off-time to its floor. Defensive — the
     * sliders constrain input, but a corrupted on-disk value or a ceiling that
     * dropped must never let a tool exceed the hardware.
     */
    fun coercedTo(maxAmplitudePercent: Int): VibrationRootToolsConfig = copy(
        extremeAmplitudePercent = extremeAmplitudePercent.coerceIn(MIN_AMPLITUDE_PERCENT, maxAmplitudePercent),
        extremeBurstMs = extremeBurstMs.coerceIn(MIN_DURATION_MS, MAX_BURST_MS),
        pwmOnMicros = pwmOnMicros.coerceIn(MIN_PWM_ON_MICROS, MAX_PWM_MICROS),
        pwmOffMicros = pwmOffMicros.coerceIn(MIN_PWM_OFF_MICROS, MAX_PWM_MICROS),
        pwmPulses = pwmPulses.coerceIn(MIN_PWM_PULSES, MAX_PWM_PULSES),
        dualPhaseOffsetMicros = dualPhaseOffsetMicros.coerceIn(0L, MAX_PWM_MICROS),
        rumbleDurationMs = rumbleDurationMs.coerceIn(MIN_DURATION_MS, MAX_RUMBLE_MS),
        rumbleAmplitudePercent = rumbleAmplitudePercent.coerceIn(MIN_AMPLITUDE_PERCENT, maxAmplitudePercent),
    )

    companion object {
        // ─── Field defaults (the legacy RootedExtrasSections demo presets) ──
        const val DEFAULT_AMPLITUDE_PERCENT: Int = 100
        const val DEFAULT_BURST_MS: Long = 2_500L
        const val DEFAULT_PWM_ON_MICROS: Long = 8_000L
        const val DEFAULT_PWM_OFF_MICROS: Long = 12_000L
        const val DEFAULT_PWM_PULSES: Int = 20
        const val DEFAULT_DUAL_PHASE_MICROS: Long = 5_000L
        const val DEFAULT_RUMBLE_MS: Long = 60_000L
        const val DEFAULT_RUMBLE_AMPLITUDE_PERCENT: Int = 35

        // ─── Slider bounds (domain ranges, not layout tokens) ───────────────
        const val MIN_AMPLITUDE_PERCENT: Int = 1
        // Amplitude MAX is the live maxAmplitudePercentFlow, NOT a constant.
        const val MIN_DURATION_MS: Long = 100L
        /** Extreme-amplitude burst hard cap — mirrors
         *  `:feature:vibration-rooted` `EXTREME_AMPLITUDE_BURST_CAP_MS` (3 s). */
        const val MAX_BURST_MS: Long = 3_000L
        /** Sustained-rumble hard cap — mirrors `SUSTAINED_RUMBLE_HARD_CAP_MS`
         *  (5 min). */
        const val MAX_RUMBLE_MS: Long = 5L * 60L * 1000L
        const val MIN_PWM_ON_MICROS: Long = 100L
        /** Direct-PWM off-time floor — mirrors `DIRECT_PWM_MIN_OFF_MICROS`. */
        const val MIN_PWM_OFF_MICROS: Long = 5_000L
        const val MAX_PWM_MICROS: Long = 1_000_000L
        const val MIN_PWM_PULSES: Int = 1
        const val MAX_PWM_PULSES: Int = 200
    }
}
