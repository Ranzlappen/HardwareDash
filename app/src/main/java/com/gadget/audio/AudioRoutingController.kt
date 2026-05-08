package com.gadget.audio

/**
 * Rooted-only Audio routing surface. Standard flavor returns
 * [AudioRoutingControllerResult.Unsupported] for every method.
 *
 * Privileged paths: `cmd audio set-stream-volume` past the safe-listening
 * ceiling (clamped to 130 % of `getStreamMaxVolume(stream)`),
 * `cmd audio set-route` (force speaker / BT-SCO), `cmd audio set-stream-mute`
 * across an internal allow-list (refuses ACCESSIBILITY / DTMF /
 * VOICE_CALL), and a read-only `cmd audio dump` snapshot.
 *
 * The mixer-side mute path reuses the public Batch-4
 * [com.gadget.microphone.AlsaMixerControl] so the snapshot+restore
 * already verified by the Batch-4 microphone controller is shared.
 */
interface AudioRoutingController {

    /**
     * Bypasses the Android safe-listening ceiling on a single stream.
     * Refuses [AudioStreamType.VOICE_CALL] regardless of caller (call
     * recording laws / emergency-services concerns). Snapshot+restore
     * in `NonCancellable` finally; 60 s active-window auto-cutoff via
     * `withTimeoutOrNull`.
     */
    suspend fun bypassStreamVolumeCap(config: StreamVolumeBypassConfig): AudioRoutingControllerResult

    /**
     * Force-routes audio to [ForceRoutingConfig.target]. Records the prior
     * routing state (`isSpeakerphoneOn`, `isBluetoothScoOn`, `mode`) under
     * `audio-policy://routing/<timestamp>` so revert is reliable even
     * after process kill.
     */
    suspend fun forceRouting(config: ForceRoutingConfig): AudioRoutingControllerResult

    /**
     * Mutes an allow-listed stream set. ACCESSIBILITY and DTMF are
     * always preserved; VOICE_CALL is always preserved. Hard 60 s
     * active-window regardless of the requested duration. Reuses
     * [com.gadget.microphone.AlsaMixerControl] for the mixer-side path
     * so revert still works after process kill.
     */
    suspend fun muteAllStreams(config: MuteAllStreamsConfig): AudioRoutingControllerResult

    /** Read-only `cmd audio dump` snapshot, tail-capped to 8 KB. */
    suspend fun dumpAudioPolicy(): AudioRoutingControllerResult

    /** Reverts every Audio-routing-surface mutation registered with the log. */
    suspend fun resetAllAudioRoutingMutations(): AudioRoutingControllerResult

    /**
     * Auto-revert path called on `MicScreen` dispose. Filters by
     * `cmd-audio://` + `audio-policy://` prefixes.
     */
    suspend fun revertOnScreenExit(): AudioRoutingControllerResult
}
