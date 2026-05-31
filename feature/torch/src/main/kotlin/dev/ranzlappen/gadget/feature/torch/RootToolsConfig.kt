package dev.ranzlappen.gadget.feature.torch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * User-tunable parameters for the four rooted Torch tools, persisted by
 * [RootToolsConfigRepository] so a chosen brightness / strobe shape survives
 * app restarts.
 *
 * Replaces the hardcoded one-tap presets that used to live as `ROOT_*`
 * constants in [TorchViewModel]: the screen's [components.RootToolsCard] now
 * edits this record through sliders + a toggle, and the four run buttons
 * dispatch with these values via [TorchRootCapabilities].
 *
 * The companion holds the field defaults (the old preset values verbatim) and
 * the slider bounds. The boost-brightness **maximum** is deliberately NOT a
 * constant here — it is the live ceiling from
 * [TorchRootCapabilities.maxBrightnessPercentFlow] (100 stock, up to 150 on a
 * rooted device with a usable LED node), so the slider can never request a
 * boost the hardware can't deliver. Use [coercedTo] to clamp a candidate
 * record against that live ceiling (and the thermal hard ceiling) before
 * persisting.
 *
 * `@Serializable` for the DataStore JSON; `@Immutable` so Compose skips
 * recompositions when the record is structurally unchanged.
 */
@Serializable
@Immutable
data class TorchRootToolsConfig(
    /** Boost-brightness target, percent of stock `max_brightness`. Clamped to
     *  the live [TorchRootCapabilities.maxBrightnessPercentFlow] ceiling. */
    val boostBrightnessPercent: Int = DEFAULT_BOOST_PERCENT,
    /** Duty-cycle strobe frequency (Hz). */
    val dutyFrequencyHz: Int = DEFAULT_STROBE_HZ,
    /** Duty-cycle strobe on-fraction (percent of each period). */
    val dutyPercent: Int = DEFAULT_DUTY_PERCENT,
    /** Duty-cycle strobe run duration (ms). */
    val dutyDurationMs: Long = DEFAULT_STROBE_DURATION_MS,
    /** Multi-LED activation duration (ms). */
    val multiLedDurationMs: Long = DEFAULT_MULTILED_DURATION_MS,
    /** Whether multi-LED also lights the screen. */
    val multiLedIncludeScreen: Boolean = false,
    /** Thermal-override strobe frequency (Hz). */
    val thermalFrequencyHz: Int = DEFAULT_STROBE_HZ,
    /** Thermal-override strobe on-fraction (percent of each period). */
    val thermalDutyPercent: Int = DEFAULT_DUTY_PERCENT,
    /** Thermal-override run duration (ms), capped at [MAX_THERMAL_DURATION_MS]. */
    val thermalDurationMs: Long = DEFAULT_THERMAL_DURATION_MS,
) {
    /**
     * Return a copy clamped to the device's real limits: brightness into
     * `[MIN_BRIGHTNESS_PERCENT, maxBrightnessPercent]` (the live ceiling) and
     * thermal duration to `[MIN_DURATION_MS, MAX_THERMAL_DURATION_MS]`; the
     * other ranged fields clamp to their static slider bounds. Defensive — the
     * sliders already constrain input, but a corrupted on-disk value or a
     * ceiling that dropped (LED node lost) must never let a tool request more
     * than the hardware allows.
     */
    fun coercedTo(maxBrightnessPercent: Int): TorchRootToolsConfig = copy(
        boostBrightnessPercent = boostBrightnessPercent
            .coerceIn(MIN_BRIGHTNESS_PERCENT, maxBrightnessPercent),
        dutyFrequencyHz = dutyFrequencyHz.coerceIn(MIN_HZ, MAX_HZ),
        dutyPercent = dutyPercent.coerceIn(MIN_DUTY, MAX_DUTY),
        dutyDurationMs = dutyDurationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
        multiLedDurationMs = multiLedDurationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
        thermalFrequencyHz = thermalFrequencyHz.coerceIn(MIN_HZ, MAX_HZ),
        thermalDutyPercent = thermalDutyPercent.coerceIn(MIN_DUTY, MAX_DUTY),
        thermalDurationMs = thermalDurationMs.coerceIn(MIN_DURATION_MS, MAX_THERMAL_DURATION_MS),
    )

    companion object {
        // ─── Field defaults (the former TorchViewModel ROOT_* presets) ──────
        const val DEFAULT_BOOST_PERCENT: Int = 150
        const val DEFAULT_STROBE_HZ: Int = 30
        const val DEFAULT_DUTY_PERCENT: Int = 20
        const val DEFAULT_STROBE_DURATION_MS: Long = 5_000L
        const val DEFAULT_MULTILED_DURATION_MS: Long = 3_000L
        const val DEFAULT_THERMAL_DURATION_MS: Long = 5_000L

        // ─── Slider bounds (domain ranges, not layout tokens) ───────────────
        /** Brightness floor = stock ceiling. The brightness MAX is the live
         *  [TorchRootCapabilities.maxBrightnessPercentFlow], NOT a constant. */
        const val MIN_BRIGHTNESS_PERCENT: Int = 100
        const val MIN_HZ: Int = 1
        const val MAX_HZ: Int = 60
        const val MIN_DUTY: Int = 5
        const val MAX_DUTY: Int = 95
        const val MIN_DURATION_MS: Long = 500L
        const val MAX_DURATION_MS: Long = 30_000L

        /**
         * Hard ceiling for the thermal-override duration slider. Mirrors
         * `THERMAL_OVERRIDE_HARD_CEILING_MILLIS` (45s) in
         * `:feature:torch-rooted`'s `ThermalOverrideController` — keep the two
         * in sync. The rooted impl clamps to its own ceiling downstream
         * regardless; bounding the slider here means the UI never offers a
         * duration the hardware would silently truncate.
         */
        const val MAX_THERMAL_DURATION_MS: Long = 45_000L
    }
}
