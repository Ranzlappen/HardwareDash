package com.gadget.audio

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Audio routing controller. Every privileged method
 * returns [AudioRoutingControllerResult.Unsupported] — there is no
 * privileged shell in this APK so `cmd audio set-stream-volume` /
 * `set-route` / `set-stream-mute` / `dump` are physically impossible.
 */
@Singleton
class StandardAudioRoutingController @Inject constructor() : AudioRoutingController {

    override suspend fun bypassStreamVolumeCap(
        config: StreamVolumeBypassConfig,
    ): AudioRoutingControllerResult = AudioRoutingControllerResult.Unsupported

    override suspend fun forceRouting(
        config: ForceRoutingConfig,
    ): AudioRoutingControllerResult = AudioRoutingControllerResult.Unsupported

    override suspend fun muteAllStreams(
        config: MuteAllStreamsConfig,
    ): AudioRoutingControllerResult = AudioRoutingControllerResult.Unsupported

    override suspend fun dumpAudioPolicy(): AudioRoutingControllerResult =
        AudioRoutingControllerResult.Unsupported

    override suspend fun resetAllAudioRoutingMutations(): AudioRoutingControllerResult =
        AudioRoutingControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): AudioRoutingControllerResult =
        AudioRoutingControllerResult.ResetCompleted(restored = 0, failed = 0)
}
