package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig

/**
 * Stateless view-state container consumed by [VibrationScreenContent]. Produced
 * by [VibrationViewModel.state] from the controller state, the saved patterns,
 * the rooted-tool config + availability, and the persisted collapse state.
 * Mirror of `TorchScreenState`.
 */
@Immutable
data class VibrationScreenState(
    val vibration: VibrationState,
    /** One-shot control's pending amplitude (percent) + duration (ms). */
    val oneShotAmplitudePercent: Int = DEFAULT_ONESHOT_AMPLITUDE,
    val oneShotDurationMs: Long = DEFAULT_ONESHOT_DURATION_MS,
    /** The current drawn pattern draft (intensity 0..1 samples), empty if none. */
    val patternDraft: List<Float> = emptyList(),
    val patternLoop: Boolean = false,
    val savedPatterns: List<VibrationPattern> = emptyList(),
    val widgets: List<SavedVibrationWidget> = emptyList(),
    val rootAvailability: VibrationRootAvailability = VibrationRootAvailability.Unavailable,
    val rootTools: VibrationRootToolsConfig = VibrationRootToolsConfig(),
    /** Live amplitude ceiling driving the rooted amplitude sliders' max. */
    val maxAmplitudePercent: Int = VibrationRootToolsConfig.MIN_AMPLITUDE_PERCENT,
    val expandedSections: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        const val DEFAULT_ONESHOT_AMPLITUDE = 60
        const val DEFAULT_ONESHOT_DURATION_MS = 300L

        /** First-emission placeholder used before the flows emit. */
        val Initial = VibrationScreenState(vibration = VibrationState())
    }
}

/** A single persisted widget — `appWidgetId` keyed [VibrationWidgetConfig]. */
@Immutable
data class SavedVibrationWidget(
    val appWidgetId: Int,
    val config: VibrationWidgetConfig,
)
