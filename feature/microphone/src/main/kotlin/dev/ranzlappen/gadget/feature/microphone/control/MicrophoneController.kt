package dev.ranzlappen.gadget.feature.microphone.control


/**
 * Rooted-only Microphone capability surface. Bypasses `android.media.AudioRecord`
 * for the extreme-tier operations — direct ALSA mixer writes for gain /
 * effects toggles, `tinycap` PCM reads for sub-AudioRecord latency, and
 * `pm grant CAPTURE_AUDIO_OUTPUT` for system audio capture.
 *
 * Standard flavor returns [MicrophoneControllerResult.Unsupported] for
 * every method.
 *
 * The interface deliberately exposes ONLY extreme-tier operations.
 * Baseline mic capture continues to flow through `AudioRecord` in
 * `MicScreen` and `VoiceRecordService`.
 */
interface MicrophoneController {

    /**
     * Boosts mic gain via direct ALSA mixer write. Hard +30 dB raw cap,
     * hard 60 s per-call ceiling. Mixer values are snapshotted and
     * restored in a `NonCancellable` finally so a cancelled coroutine
     * leaves the mic in its original state.
     */
    suspend fun gainBoost(config: GainBoostConfig): MicrophoneControllerResult

    /**
     * Reads raw PCM via `tinycap` from `/dev/snd/pcmC0D*c`. Sample rate
     * and format are device-dependent; the impl validates the request
     * against `/proc/asound/.../caps` before opening. Min 5 ms read
     * window; max 30 s per call.
     */
    suspend fun directPcm(config: DirectPcmConfig): MicrophoneControllerResult

    /**
     * Requests an unusual sample rate (e.g. 192 kHz for ultrasonic
     * capture). Requires explicit confirm — some kernels lock up on
     * unsupported rates and need a reboot.
     */
    suspend fun customSampleRate(config: CustomRateConfig): MicrophoneControllerResult

    /**
     * Captures from every available input PCM node simultaneously. Hard
     * 30 s ceiling, max 3 concurrent streams.
     */
    suspend fun multiMicRaw(config: MultiMicConfig): MicrophoneControllerResult

    /**
     * Disables hardware noise-suppression / AGC / AEC by toggling the
     * relevant ALSA mixer controls. Snapshotted and restored.
     */
    suspend fun disableEffects(): MicrophoneControllerResult

    /**
     * Enables system-audio loopback capture by granting
     * `CAPTURE_AUDIO_OUTPUT` to the app via `pm grant`. **Mandatory
     * legal-warning confirm** — call recording is illegal in many
     * jurisdictions. Hard 5-minute per-call ceiling. Permission is
     * revoked in finally.
     */
    suspend fun systemAudioCapture(durationMillis: Long): MicrophoneControllerResult
}
