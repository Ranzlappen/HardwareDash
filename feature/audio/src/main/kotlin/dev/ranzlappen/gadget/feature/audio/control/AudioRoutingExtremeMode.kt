package dev.ranzlappen.gadget.feature.audio.control


/**
 * Stream identifier for stream-volume bypass + mute operations. Maps to
 * `AudioManager.STREAM_*` constants inside the helper. VOICE_CALL is
 * unconditionally rejected by the bypass helper regardless of caller.
 */
enum class AudioStreamType {
    MUSIC,
    NOTIFICATION,
    RING,
    ALARM,
    SYSTEM,
    VOICE_CALL,
}

enum class AudioRoutingTarget {
    SPEAKER,
    EARPIECE,
    BLUETOOTH_SCO,
    WIRED_HEADSET,
}

/**
 * Configures a stream-volume bypass request. The helper reads
 * `getStreamMaxVolume(stream)` and clamps the effective applied index
 * to 130 % of that ceiling regardless of caller input.
 * [activeWindowMillis] is hard-capped to 60 s by the helper.
 */
data class StreamVolumeBypassConfig(
    val stream: AudioStreamType,
    val percent: Int,
    val activeWindowMillis: Long,
)

data class ForceRoutingConfig(
    val target: AudioRoutingTarget,
)

data class MuteAllStreamsConfig(
    val durationMillis: Long,
)
