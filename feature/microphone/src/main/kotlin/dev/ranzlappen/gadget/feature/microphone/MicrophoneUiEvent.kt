package dev.ranzlappen.gadget.feature.microphone

/**
 * Every user-initiated event surfaced by [MicrophoneScreenContent]. The
 * screen content flattens its public API to a single
 * `onEvent: (MicrophoneUiEvent) -> Unit`; [MicrophoneViewModel.onEvent]
 * dispatches each variant to a typed handler. Mirror of `VibrationUiEvent`.
 *
 * The custom-sample-rate and system-audio-capture rows split their "run"
 * event into a `*Request` (opens the confirm dialog) / `*Confirm` (dismisses
 * the dialog and actually dispatches) / `*Dismiss` (cancels) triplet — every
 * other row dispatches straight from its Run event.
 */
sealed interface MicrophoneUiEvent {

    // ─── Gain boost ─────────────────────────────────────────────────────
    data class GainBoostDbChange(val db: Int) : MicrophoneUiEvent
    data class GainBoostDurationChange(val durationMs: Long) : MicrophoneUiEvent
    data object GainBoostRun : MicrophoneUiEvent

    // ─── Direct PCM ─────────────────────────────────────────────────────
    data class DirectPcmDurationChange(val durationMs: Long) : MicrophoneUiEvent
    data object DirectPcmRun : MicrophoneUiEvent

    // ─── Custom sample rate (kernel-lockup-risk confirm) ────────────────
    data class CustomSampleRateHzChange(val hz: Int) : MicrophoneUiEvent
    data class CustomSampleRateDurationChange(val durationMs: Long) : MicrophoneUiEvent
    data object CustomSampleRateRequest : MicrophoneUiEvent
    data object CustomSampleRateConfirm : MicrophoneUiEvent
    data object CustomSampleRateDismiss : MicrophoneUiEvent

    // ─── Multi-mic raw ──────────────────────────────────────────────────
    data class MultiMicDurationChange(val durationMs: Long) : MicrophoneUiEvent
    data class MultiMicStreamsChange(val streams: Int) : MicrophoneUiEvent
    data object MultiMicRun : MicrophoneUiEvent

    // ─── Disable hardware noise suppression ─────────────────────────────
    data object DisableEffectsToggle : MicrophoneUiEvent

    // ─── System audio capture (mandatory legal-warning confirm) ─────────
    data class SystemAudioCaptureDurationChange(val durationMs: Long) : MicrophoneUiEvent
    data object SystemAudioCaptureRequest : MicrophoneUiEvent
    data object SystemAudioCaptureConfirm : MicrophoneUiEvent
    data object SystemAudioCaptureDismiss : MicrophoneUiEvent
}
