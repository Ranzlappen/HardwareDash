package com.gadget.audio

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val AUDIO_RESET_PREFIXES = listOf("cmd-audio://", "audio-policy://")
private val AUDIO_SCREEN_EXIT_PREFIXES = listOf("cmd-audio://", "audio-policy://")

/**
 * Rooted-flavor Audio routing controller. Wires the safety gate to the
 * four audio helpers. Auto-revert and reset filter the
 * `cmd-audio://` and `audio-policy://` prefixes so a stream-volume
 * bypass / forced route / mute is undone when the user leaves
 * MicScreen even after the per-helper finally has already restored
 * (idempotent).
 */
@Singleton
class RootedAudioRoutingController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val volumeBypass: StreamVolumeBypassHelper,
    private val routeHelper: AudioRouteHelper,
    private val muteHelper: AudioMuteHelper,
    private val dumpHelper: AudioDumpHelper,
    private val mutationLog: SysfsMutationLog,
) : AudioRoutingController {

    override suspend fun bypassStreamVolumeCap(
        config: StreamVolumeBypassConfig,
    ): AudioRoutingControllerResult = runGated(RootFeatureKey.AudioStreamVolumeBypass) {
        when (val outcome = volumeBypass.bypass(config)) {
            VolumeBypassOutcome.Unavailable -> AudioRoutingControllerResult.Unsupported
            VolumeBypassOutcome.WriteFailed ->
                AudioRoutingControllerResult.HardwareError("cmd audio set-stream-volume rejected")
            is VolumeBypassOutcome.Refused ->
                AudioRoutingControllerResult.HardwareError(outcome.message)
            is VolumeBypassOutcome.Applied -> AudioRoutingControllerResult.VolumeSnapshot(
                stream = config.stream,
                originalIndex = outcome.originalIndex,
                appliedIndex = outcome.appliedIndex,
                maxIndex = outcome.maxIndex,
            )
        }
    }

    override suspend fun forceRouting(
        config: ForceRoutingConfig,
    ): AudioRoutingControllerResult = runGated(RootFeatureKey.AudioForceRouting) {
        when (val outcome = routeHelper.applyRoute(config.target)) {
            RouteOutcome.Unavailable -> AudioRoutingControllerResult.Unsupported
            is RouteOutcome.Applied -> AudioRoutingControllerResult.RoutingSnapshot(
                priorTarget = outcome.priorTarget,
                appliedTarget = outcome.appliedTarget,
            )
        }
    }

    override suspend fun muteAllStreams(
        config: MuteAllStreamsConfig,
    ): AudioRoutingControllerResult = runGated(RootFeatureKey.AudioMuteAllStreams) {
        when (val outcome = muteHelper.muteAll(config.durationMillis)) {
            MuteOutcome.Unavailable -> AudioRoutingControllerResult.Unsupported
            is MuteOutcome.Applied -> AudioRoutingControllerResult.Ok(
                statusNote = "muted ${outcome.mutedStreams.size} streams for " +
                    "${outcome.durationMillis / 1000}s",
            )
        }
    }

    override suspend fun dumpAudioPolicy(): AudioRoutingControllerResult =
        runGated(RootFeatureKey.AudioDumpAudioPolicy) {
            val excerpt = dumpHelper.snapshot()
                ?: return@runGated AudioRoutingControllerResult.HardwareError(
                    "cmd audio dump failed",
                )
            AudioRoutingControllerResult.AudioDumpExcerpt(excerpt)
        }

    override suspend fun resetAllAudioRoutingMutations(): AudioRoutingControllerResult {
        val outcome = mutationLog.revertAll(AUDIO_RESET_PREFIXES)
        return AudioRoutingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): AudioRoutingControllerResult {
        val outcome = mutationLog.revertAll(AUDIO_SCREEN_EXIT_PREFIXES)
        return AudioRoutingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> AudioRoutingControllerResult,
    ): AudioRoutingControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is AudioRoutingControllerResult.Ok ||
                it is AudioRoutingControllerResult.VolumeSnapshot ||
                it is AudioRoutingControllerResult.RoutingSnapshot ||
                it is AudioRoutingControllerResult.AudioDumpExcerpt
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> AudioRoutingControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            AudioRoutingControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> AudioRoutingControllerResult.Unsupported
    }
}
