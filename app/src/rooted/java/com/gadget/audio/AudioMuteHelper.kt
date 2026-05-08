package com.gadget.audio

import android.media.AudioManager
import com.gadget.microphone.AlsaMixerControl
import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

internal const val MUTE_HARD_CEILING_MILLIS = 60_000L

private val SAFE_MUTE_STREAMS = listOf(
    AudioManager.STREAM_MUSIC,
    AudioManager.STREAM_NOTIFICATION,
    AudioManager.STREAM_RING,
    AudioManager.STREAM_SYSTEM,
)

private val MIXER_MUTE_CONTROLS = listOf(
    "Master Mute",
    "Speaker Switch",
    "Headphone Switch",
    "DAC1 Switch",
)

/**
 * Mutes an allow-listed stream set via `cmd audio set-stream-mute`.
 * ACCESSIBILITY and DTMF are unconditionally preserved (omitted from
 * [SAFE_MUTE_STREAMS]); VOICE_CALL is also preserved. Hard 60 s
 * active-window via [withTimeoutOrNull].
 *
 * Reuses the public Batch-4 [AlsaMixerControl] for the mixer-side path
 * so the snapshot+restore lives in one place. The mutation log
 * `cmd-audio://mute/<stream>` entry covers the cmd-shell side.
 */
@Singleton
class AudioMuteHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
    private val mixer: AlsaMixerControl,
) {
    suspend fun muteAll(durationMillis: Long): MuteOutcome {
        val effectiveDuration = durationMillis.coerceAtMost(MUTE_HARD_CEILING_MILLIS)
        val pseudoPaths = mutableListOf<String>()
        val mixerSnapshot = mixer.snapshot(MIXER_MUTE_CONTROLS)
        for (stream in SAFE_MUTE_STREAMS) {
            val pseudo = "cmd-audio://mute/$stream"
            mutationLog.register(pseudo, "0")
            pseudoPaths += pseudo
            shell.exec("cmd audio set-stream-mute $stream true")
        }
        if (mixerSnapshot != null) {
            for (control in mixerSnapshot.controls.keys) {
                mixer.setControlValue(control, "0")
            }
        }
        try {
            withTimeoutOrNull(effectiveDuration) { delay(effectiveDuration) }
        } finally {
            withContext(NonCancellable) {
                for (stream in SAFE_MUTE_STREAMS) {
                    shell.exec("cmd audio set-stream-mute $stream false")
                }
                for (pseudo in pseudoPaths) {
                    mutationLog.unregister(pseudo)
                }
                if (mixerSnapshot != null) mixer.restore(mixerSnapshot)
            }
        }
        return MuteOutcome.Applied(
            mutedStreams = SAFE_MUTE_STREAMS.toList(),
            durationMillis = effectiveDuration,
        )
    }
}

sealed class MuteOutcome {
    data object Unavailable : MuteOutcome()
    data class Applied(
        val mutedStreams: List<Int>,
        val durationMillis: Long,
    ) : MuteOutcome()
}
