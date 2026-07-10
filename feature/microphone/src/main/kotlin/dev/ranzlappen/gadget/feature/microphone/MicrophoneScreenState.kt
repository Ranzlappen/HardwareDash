package dev.ranzlappen.gadget.feature.microphone

import androidx.compose.runtime.Immutable

/**
 * Stateless view-state container consumed by [MicrophoneScreenContent].
 * Produced by [MicrophoneViewModel.state]. Every field pairs a tool's pending
 * (session-only) input with an `*InFlight` flag driving its Run button /
 * loading spinner. There is no persisted config store (unlike Vibration's
 * root-tools) — these are diagnostic one-shots, not settings worth
 * remembering across sessions.
 *
 * [disableEffectsInFlight] and [systemAudioCaptureInFlight] double as the
 * two "toggle" rows' checked state: both underlying controller calls
 * snapshot-then-restore internally (see `RootedMicrophoneController`), so
 * there is no persistent "effects disabled" / "system audio capturing" state
 * to reflect — the switch is checked exactly while its coroutine is in
 * flight and snaps back once the (self-restoring) call completes.
 */
@Immutable
data class MicrophoneScreenState(
    val isRootedFlavor: Boolean = false,

    // ─── Gain boost ─────────────────────────────────────────────────────
    val gainBoostDb: Int = DEFAULT_GAIN_BOOST_DB,
    val gainBoostDurationMs: Long = DEFAULT_GAIN_BOOST_DURATION_MS,
    val gainBoostInFlight: Boolean = false,

    // ─── Direct PCM ─────────────────────────────────────────────────────
    val directPcmDurationMs: Long = DEFAULT_DIRECT_PCM_DURATION_MS,
    val directPcmInFlight: Boolean = false,

    // ─── Custom sample rate ─────────────────────────────────────────────
    val customSampleRateHz: Int = DEFAULT_CUSTOM_SAMPLE_RATE_HZ,
    val customSampleRateDurationMs: Long = DEFAULT_CUSTOM_RATE_DURATION_MS,
    val customSampleRateInFlight: Boolean = false,
    val showCustomSampleRateConfirm: Boolean = false,

    // ─── Multi-mic raw ──────────────────────────────────────────────────
    val multiMicDurationMs: Long = DEFAULT_MULTI_MIC_DURATION_MS,
    val multiMicStreams: Int = DEFAULT_MULTI_MIC_STREAMS,
    val multiMicInFlight: Boolean = false,

    // ─── Disable hardware noise suppression ─────────────────────────────
    val disableEffectsInFlight: Boolean = false,

    // ─── System audio capture ───────────────────────────────────────────
    val systemAudioCaptureDurationMs: Long = DEFAULT_SYSTEM_AUDIO_DURATION_MS,
    val systemAudioCaptureInFlight: Boolean = false,
    val showSystemAudioCaptureConfirm: Boolean = false,
) {
    companion object {
        const val DEFAULT_GAIN_BOOST_DB = 10
        const val MIN_GAIN_BOOST_DB = 0
        const val MAX_GAIN_BOOST_DB = 30
        const val DEFAULT_GAIN_BOOST_DURATION_MS = 5_000L
        const val MAX_GAIN_BOOST_DURATION_MS = 60_000L

        const val DEFAULT_DIRECT_PCM_DURATION_MS = 3_000L
        const val MIN_DIRECT_PCM_DURATION_MS = 100L
        const val MAX_DIRECT_PCM_DURATION_MS = 30_000L
        // Fixed capture format for the diagnostic PCM row — device-dependent
        // caps are validated by the impl itself; a single sane default keeps
        // this row a one-tap diagnostic rather than a mixer-config form.
        const val DIRECT_PCM_SAMPLE_RATE_HZ = 48_000
        const val DIRECT_PCM_CHANNEL_COUNT = 1
        const val DIRECT_PCM_BITS_PER_SAMPLE = 16

        const val MIN_CUSTOM_SAMPLE_RATE_HZ = 4_000
        const val MAX_CUSTOM_SAMPLE_RATE_HZ = 384_000
        const val DEFAULT_CUSTOM_SAMPLE_RATE_HZ = 192_000
        const val DEFAULT_CUSTOM_RATE_DURATION_MS = 5_000L
        const val MAX_CUSTOM_RATE_DURATION_MS = 30_000L

        const val DEFAULT_MULTI_MIC_DURATION_MS = 10_000L
        const val MAX_MULTI_MIC_DURATION_MS = 30_000L
        const val DEFAULT_MULTI_MIC_STREAMS = 3
        const val MIN_MULTI_MIC_STREAMS = 1
        const val MAX_MULTI_MIC_STREAMS = 3

        const val DEFAULT_SYSTEM_AUDIO_DURATION_MS = 60_000L
        const val MIN_SYSTEM_AUDIO_DURATION_MS = 5_000L
        const val MAX_SYSTEM_AUDIO_DURATION_MS = 300_000L

        /** First-emission placeholder used before the ViewModel's flow emits. */
        val Initial = MicrophoneScreenState()
    }
}
