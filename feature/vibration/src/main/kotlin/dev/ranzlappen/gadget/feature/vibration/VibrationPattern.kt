package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A saved haptic waveform — alternating off/on segments with per-segment
 * strengths, the modular successor to the legacy draw-canvas patterns.
 *
 * - [timingsMillis] / [amplitudes] are parallel arrays in
 *   `VibrationEffect.createWaveform` form: index 0 is an initial **off**
 *   segment, then alternating on/off. [amplitudes] is 0..255
 *   (or [VibrationController.AMPLITUDE_DEFAULT]) and only honoured when the
 *   device `hasAmplitudeControl`.
 * - [id] is a stable key; [name] is the user-facing label.
 *
 * `@Serializable` for DataStore persistence (lists, not arrays, so the
 * generated serializer + structural equality behave); `@Immutable` for Compose.
 */
@Serializable
@Immutable
data class VibrationPattern(
    val id: String,
    val name: String,
    val timingsMillis: List<Long>,
    val amplitudes: List<Int>,
) {
    /** Total play time of one pass (sum of all segment durations). */
    val totalMillis: Long get() = timingsMillis.sum()

    /** Peak commanded strength as a percent (for the modelled monitor signal). */
    val peakPercent: Int
        get() = amplitudes.maxOrNull()?.let { raw ->
            if (raw <= 0) 100 else (raw * 100 / 255).coerceIn(1, 100)
        } ?: 100

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}
