package com.gadget.microphone

/**
 * Mic gain boost via ALSA mixer write. [boostDb] is clamped to a hard +30 dB
 * raw mixer ceiling and [durationMillis] is hard-capped at 60 seconds inside
 * the impl. Original mixer values are snapshotted before the write and
 * restored in a `NonCancellable` finally.
 */
data class GainBoostConfig(
    val boostDb: Int,
    val durationMillis: Long,
)

/**
 * Direct PCM read from `/dev/snd/pcmC0D*c` via tinycap. [sampleRate] and
 * [bitsPerSample] are device-dependent; the impl validates against
 * `/proc/asound/.../caps` before opening. Min 5 ms read window;
 * [durationMillis] hard-capped at 30 seconds.
 */
data class DirectPcmConfig(
    val sampleRate: Int,
    val channelCount: Int,
    val bitsPerSample: Int,
    val durationMillis: Long,
)

/**
 * Custom sample rate request (e.g. 192 kHz for ultrasonic). Some kernels
 * lock up on unsupported rates and need a reboot — the descriptor for
 * `MicCustomSampleRate` therefore requires explicit-confirm. The impl
 * sanity-checks against `/proc/asound/cards` before writing.
 */
data class CustomRateConfig(
    val targetSampleRate: Int,
    val durationMillis: Long,
)

/**
 * Multi-mic raw capture across all available capture PCM nodes (back,
 * bottom, earpiece, speakerphone). Hard 30 s ceiling; max 3 concurrent
 * streams.
 */
data class MultiMicConfig(
    val durationMillis: Long,
    val maxStreams: Int = 3,
)
