package dev.ranzlappen.gadget.feature.microphone.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Microphone controller. Every extreme-tier method returns
 * [MicrophoneControllerResult.Unsupported] — the standard APK has no
 * privileged shell, so direct ALSA mixer writes / `tinycap` reads /
 * permission granting are impossible regardless of perms.
 */
@Singleton
class StandardMicrophoneController @Inject constructor() : MicrophoneController {

    override suspend fun gainBoost(config: GainBoostConfig): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported

    override suspend fun directPcm(config: DirectPcmConfig): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported

    override suspend fun customSampleRate(config: CustomRateConfig): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported

    override suspend fun multiMicRaw(config: MultiMicConfig): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported

    override suspend fun disableEffects(): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported

    override suspend fun systemAudioCapture(durationMillis: Long): MicrophoneControllerResult =
        MicrophoneControllerResult.Unsupported
}
