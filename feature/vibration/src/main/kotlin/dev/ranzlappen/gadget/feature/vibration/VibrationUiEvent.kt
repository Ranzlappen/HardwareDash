package dev.ranzlappen.gadget.feature.vibration

/**
 * Every user-initiated event surfaced by [VibrationScreenContent]. The screen
 * content flattens its public API to a single `onEvent: (VibrationUiEvent) ->
 * Unit`; [VibrationViewModel.onEvent] dispatches each variant to a typed
 * handler. Mirror of `TorchUiEvent`.
 */
sealed interface VibrationUiEvent {

    // ─── Standard controls ───────────────────────────────────────────────
    data class PredefinedClick(val effect: VibrationPredefinedEffect) : VibrationUiEvent
    data object OneShot : VibrationUiEvent
    data class OneShotAmplitudeChange(val percent: Int) : VibrationUiEvent
    data class OneShotDurationChange(val durationMs: Long) : VibrationUiEvent
    data object OneShotCommit : VibrationUiEvent
    data object Stop : VibrationUiEvent

    // ─── Pattern builder ─────────────────────────────────────────────────
    /** Replace the in-progress drawn pattern (samples are intensity 0..1 over
     *  the fixed canvas window). */
    data class PatternDraftChange(val samples: List<Float>) : VibrationUiEvent
    data class PatternLoopChange(val loop: Boolean) : VibrationUiEvent
    data object PatternPlay : VibrationUiEvent
    data object PatternClear : VibrationUiEvent
    data class PatternSaveRequest(val name: String) : VibrationUiEvent
    data class PatternPlaySaved(val pattern: VibrationPattern) : VibrationUiEvent
    data class PatternDelete(val pattern: VibrationPattern) : VibrationUiEvent

    // ─── Widgets ─────────────────────────────────────────────────────────
    data object AddVibrate : VibrationUiEvent
    data object AddPattern : VibrationUiEvent
    data object QuickPinVibrate : VibrationUiEvent
    data class EditWidget(val widget: SavedVibrationWidget) : VibrationUiEvent
    data class DeleteWidget(val widget: SavedVibrationWidget) : VibrationUiEvent
    data class SheetConfirmed(val config: dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig) :
        VibrationUiEvent
    data object SheetDismissed : VibrationUiEvent

    // ─── Rooted tools ────────────────────────────────────────────────────
    data object RootExtremeAmplitude : VibrationUiEvent
    data object RootDirectPwm : VibrationUiEvent
    data object RootDualActuator : VibrationUiEvent
    data object RootSustainedRumble : VibrationUiEvent
    data class RootToolsChange(val config: VibrationRootToolsConfig) : VibrationUiEvent
    data object RootToolsCommit : VibrationUiEvent

    // ─── Collapse ────────────────────────────────────────────────────────
    data class SectionToggle(val id: String) : VibrationUiEvent
}
