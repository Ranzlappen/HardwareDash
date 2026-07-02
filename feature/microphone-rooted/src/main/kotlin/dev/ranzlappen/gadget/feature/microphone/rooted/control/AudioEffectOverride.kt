package dev.ranzlappen.gadget.feature.microphone.rooted.control

import dev.ranzlappen.gadget.core.root.audio.AlsaMixerControl
import dev.ranzlappen.gadget.core.root.audio.MixerSnapshot
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disables hardware noise-suppression, automatic gain control, and
 * acoustic echo cancellation by writing zero to the relevant ALSA mixer
 * controls. Snapshots the original values via [AlsaMixerControl.snapshot]
 * before the write so a restore in `NonCancellable` finally returns the
 * mic to its baseline.
 *
 * On devices without `tinymix` or without these named controls the
 * controller surfaces [MicrophoneControllerResult.Unsupported].
 */
@Singleton
class AudioEffectOverride @Inject constructor(
    private val mixer: AlsaMixerControl,
) {
    suspend fun disable(): EffectOverrideOutcome {
        val snapshot = mixer.snapshot(EFFECT_CONTROL_CANDIDATES) ?: return EffectOverrideOutcome.Unavailable
        var anyApplied = false
        for (name in snapshot.controls.keys) {
            if (mixer.setControlValue(name, "0")) anyApplied = true
        }
        return if (anyApplied) {
            EffectOverrideOutcome.Applied(snapshot)
        } else {
            EffectOverrideOutcome.Failed(snapshot)
        }
    }

    suspend fun restore(snapshot: MixerSnapshot) {
        mixer.restore(snapshot)
    }

    private companion object {
        val EFFECT_CONTROL_CANDIDATES = listOf(
            "ANC Switch",
            "Noise Suppression",
            "AGC Switch",
            "AEC Switch",
            "Echo Cancel",
            "Beamforming",
        )
    }
}

sealed class EffectOverrideOutcome {
    data object Unavailable : EffectOverrideOutcome()
    data class Applied(val snapshot: MixerSnapshot) : EffectOverrideOutcome()
    data class Failed(val snapshot: MixerSnapshot) : EffectOverrideOutcome()
}
