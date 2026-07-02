package dev.ranzlappen.gadget.feature.microphone.rooted.control

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.audio.AlsaMixerControl
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.microphone.control.CustomRateConfig
import dev.ranzlappen.gadget.feature.microphone.control.DirectPcmConfig
import dev.ranzlappen.gadget.feature.microphone.control.GainBoostConfig
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult
import dev.ranzlappen.gadget.feature.microphone.control.MultiMicConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal const val MIC_GAIN_HARD_CEILING_MILLIS = 60_000L
internal const val DIRECT_PCM_HARD_CEILING_MILLIS = 30_000L
internal const val DIRECT_PCM_MIN_WINDOW_MILLIS = 5L
internal const val CUSTOM_RATE_MIN_HZ = 4_000
internal const val CUSTOM_RATE_MAX_HZ = 384_000

/**
 * Rooted-flavor Microphone controller. Sub-batch 4d wires gain boost +
 * direct PCM + custom sample rate; multi-mic / effect-disable / system
 * audio land in 4e. Until then, those methods route through the safety
 * gate (so the limiter still counts them if invoked) but return
 * [MicrophoneControllerResult.Unsupported] at the impl layer.
 */
@Singleton
class RootedMicrophoneController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
    private val paths: MicSysfsPaths,
    private val mixer: AlsaMixerControl,
    private val multiMic: MultiMicCapture,
    private val effectOverride: AudioEffectOverride,
    private val systemAudio: SystemAudioCapture,
) : MicrophoneController {

    override suspend fun gainBoost(config: GainBoostConfig): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicGainBoost) {
            val controls = mixer.gainControlNames()
            if (controls.isEmpty()) return@runGated MicrophoneControllerResult.Unsupported
            val snapshot = mixer.snapshot(controls)
                ?: return@runGated MicrophoneControllerResult.Unsupported
            val effectiveDuration = config.durationMillis.coerceAtMost(MIC_GAIN_HARD_CEILING_MILLIS)
            try {
                val applied = mixer.setGainDb(snapshot, config.boostDb)
                if (!applied) {
                    return@runGated MicrophoneControllerResult.HardwareError("no gain control accepted the write")
                }
                delay(effectiveDuration)
                MicrophoneControllerResult.Ok
            } finally {
                withContext(NonCancellable) { mixer.restore(snapshot) }
            }
        }

    override suspend fun directPcm(config: DirectPcmConfig): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicDirectPcm) {
            val surface = paths.resolve()
            val node = surface.captureDevices.firstOrNull()
                ?: return@runGated MicrophoneControllerResult.Unsupported
            val effectiveDuration = config.durationMillis
                .coerceIn(DIRECT_PCM_MIN_WINDOW_MILLIS, DIRECT_PCM_HARD_CEILING_MILLIS)
            val cardDevice = parseCardDevice(node)
                ?: return@runGated MicrophoneControllerResult.HardwareError("could not parse $node")
            val script = buildString {
                append("tinycap /dev/null ")
                append("-D ${cardDevice.card} ")
                append("-d ${cardDevice.device} ")
                append("-c ${config.channelCount} ")
                append("-r ${config.sampleRate} ")
                append("-b ${config.bitsPerSample} ")
                append("-T ${effectiveDuration / 1000}")
            }
            val result = shell.exec(script, timeoutMillis = effectiveDuration + 2_000L)
            if (result.isSuccess) {
                MicrophoneControllerResult.Ok
            } else {
                MicrophoneControllerResult.HardwareError(
                    "tinycap exit ${result.exitCode}: ${result.stderr.firstOrNull().orEmpty()}",
                )
            }
        }

    override suspend fun customSampleRate(config: CustomRateConfig): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicCustomSampleRate) {
            if (config.targetSampleRate !in CUSTOM_RATE_MIN_HZ..CUSTOM_RATE_MAX_HZ) {
                return@runGated MicrophoneControllerResult.HardwareError(
                    "rate ${config.targetSampleRate} outside [$CUSTOM_RATE_MIN_HZ..$CUSTOM_RATE_MAX_HZ]",
                )
            }
            val surface = paths.resolve()
            val node = surface.captureDevices.firstOrNull()
                ?: return@runGated MicrophoneControllerResult.Unsupported
            val cardDevice = parseCardDevice(node)
                ?: return@runGated MicrophoneControllerResult.HardwareError("could not parse $node")
            // Best-effort caps probe; we don't reject on missing /proc nodes since
            // some drivers don't populate them until a stream is open.
            shell.exec("cat /proc/asound/card${cardDevice.card}/pcm${cardDevice.device}c/sub0/hw_params 2>/dev/null")
            val effectiveDuration = config.durationMillis.coerceAtMost(DIRECT_PCM_HARD_CEILING_MILLIS)
            val script = buildString {
                append("tinycap /dev/null ")
                append("-D ${cardDevice.card} ")
                append("-d ${cardDevice.device} ")
                append("-r ${config.targetSampleRate} ")
                append("-T ${effectiveDuration / 1000}")
            }
            val result = shell.exec(script, timeoutMillis = effectiveDuration + 2_000L)
            if (result.isSuccess) {
                MicrophoneControllerResult.Ok
            } else {
                MicrophoneControllerResult.HardwareError(
                    "custom-rate tinycap exit ${result.exitCode}",
                )
            }
        }

    override suspend fun multiMicRaw(config: MultiMicConfig): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicMultiMicRaw) { multiMic.capture(config) }

    override suspend fun disableEffects(): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicNoiseFloorOverride) {
            when (val outcome = effectOverride.disable()) {
                EffectOverrideOutcome.Unavailable -> MicrophoneControllerResult.Unsupported
                is EffectOverrideOutcome.Applied -> {
                    try {
                        MicrophoneControllerResult.Ok
                    } finally {
                        withContext(NonCancellable) { effectOverride.restore(outcome.snapshot) }
                    }
                }
                is EffectOverrideOutcome.Failed -> {
                    withContext(NonCancellable) { effectOverride.restore(outcome.snapshot) }
                    MicrophoneControllerResult.HardwareError("no effect control accepted the write")
                }
            }
        }

    override suspend fun systemAudioCapture(durationMillis: Long): MicrophoneControllerResult =
        runGated(RootFeatureKey.MicSystemAudioCapture) {
            systemAudio.grantThenWait(durationMillis)
        }

    private fun parseCardDevice(node: String): CardDevice? {
        // /dev/snd/pcmC0D2c → card=0 device=2
        val match = Regex("pcmC(\\d+)D(\\d+)c").find(node) ?: return null
        return CardDevice(
            card = match.groupValues[1].toInt(),
            device = match.groupValues[2].toInt(),
        )
    }

    private data class CardDevice(val card: Int, val device: Int)

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> MicrophoneControllerResult,
    ): MicrophoneControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is MicrophoneControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> MicrophoneControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            MicrophoneControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> MicrophoneControllerResult.Unsupported
    }
}
