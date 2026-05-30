package com.gadget.audio

import android.content.Context
import android.media.AudioManager
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

internal const val VOLUME_BYPASS_OVERDRIVE_PERCENT = 130
internal const val VOLUME_BYPASS_HARD_CEILING_MILLIS = 60_000L
private const val PERCENT_DIVISOR = 100

/**
 * Bypasses the Android safe-listening ceiling on a single stream by
 * driving `cmd audio set-stream-volume` past the regulatory cap. The
 * helper:
 *  - Reads `getStreamMaxVolume(stream)` per call.
 *  - Clamps the applied index to 130 % of that ceiling.
 *  - Refuses [AudioStreamType.VOICE_CALL] regardless of caller input
 *    (call-recording laws + emergency-services concerns).
 *  - Snapshots the prior index, registers `cmd-audio://stream/<n>/volume`
 *    in the mutation log, and restores in `NonCancellable` finally.
 *  - Auto-cuts off after 60 s via [withTimeoutOrNull].
 */
@Singleton
class StreamVolumeBypassHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
    @ApplicationContext private val context: Context,
) {
    suspend fun bypass(config: StreamVolumeBypassConfig): VolumeBypassOutcome {
        if (config.stream == AudioStreamType.VOICE_CALL) {
            return VolumeBypassOutcome.Refused(
                "VOICE_CALL is permanently refused for hearing-safety + legal reasons",
            )
        }
        val streamId = mapToFrameworkStream(config.stream)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return VolumeBypassOutcome.Unavailable
        val maxIndex = audioManager.getStreamMaxVolume(streamId)
        val originalIndex = audioManager.getStreamVolume(streamId)
        val hardCeiling = (maxIndex * VOLUME_BYPASS_OVERDRIVE_PERCENT) / PERCENT_DIVISOR
        val requested = (maxIndex * config.percent.coerceAtLeast(0)) / PERCENT_DIVISOR
        val appliedIndex = requested.coerceAtMost(hardCeiling)
        val pseudoPath = "cmd-audio://stream/$streamId/volume"
        val effectiveWindow = config.activeWindowMillis.coerceAtMost(VOLUME_BYPASS_HARD_CEILING_MILLIS)
        mutationLog.register(pseudoPath, originalIndex.toString())
        val write = shell.exec("cmd audio set-stream-volume $streamId $appliedIndex 0")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return VolumeBypassOutcome.WriteFailed
        }
        try {
            withTimeoutOrNull(effectiveWindow) { delay(effectiveWindow) }
        } finally {
            withContext(NonCancellable) {
                shell.exec("cmd audio set-stream-volume $streamId $originalIndex 0")
                mutationLog.unregister(pseudoPath)
            }
        }
        return VolumeBypassOutcome.Applied(
            originalIndex = originalIndex,
            appliedIndex = appliedIndex,
            maxIndex = maxIndex,
        )
    }

    private fun mapToFrameworkStream(type: AudioStreamType): Int = when (type) {
        AudioStreamType.MUSIC -> AudioManager.STREAM_MUSIC
        AudioStreamType.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        AudioStreamType.RING -> AudioManager.STREAM_RING
        AudioStreamType.ALARM -> AudioManager.STREAM_ALARM
        AudioStreamType.SYSTEM -> AudioManager.STREAM_SYSTEM
        AudioStreamType.VOICE_CALL -> AudioManager.STREAM_VOICE_CALL
    }
}

sealed class VolumeBypassOutcome {
    data object Unavailable : VolumeBypassOutcome()
    data object WriteFailed : VolumeBypassOutcome()
    data class Refused(val message: String) : VolumeBypassOutcome()
    data class Applied(
        val originalIndex: Int,
        val appliedIndex: Int,
        val maxIndex: Int,
    ) : VolumeBypassOutcome()
}
