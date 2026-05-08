package com.gadget.torch

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val BRIGHTNESS_BOOST_CAP_PERCENT = 150
private const val PERCENT_DENOMINATOR = 100

/**
 * Rooted-flavor Torch controller. Each public method delegates to a
 * dedicated `internal` helper for the actual hardware work, with
 * [RootSafetyGate] mediating capability + opt-out + rate-limit on entry.
 *
 * The thermal override path additionally enforces a hard 45-second ceiling
 * inside [ThermalOverrideController.withOverride] — see
 * [THERMAL_OVERRIDE_HARD_CEILING_MILLIS].
 */
@Singleton
class RootedTorchController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
    private val paths: TorchSysfsPaths,
    private val multiLed: MultiLedOrchestrator,
    private val strobe: DutyCycleStrobe,
    private val thermal: ThermalOverrideController,
) : TorchController {

    override suspend fun boostBrightness(percent: Int): TorchControllerResult =
        runGated(RootFeatureKey.TorchExtremeBrightness) {
            writeBoostedBrightness(percent)
        }

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long,
    ): TorchControllerResult = runGated(RootFeatureKey.TorchHighFrequencyStrobe) {
        val node = paths.resolvePrimary() ?: return@runGated TorchControllerResult.Unsupported
        strobe.run(node, frequencyHz, dutyPercent, durationMillis, phaseOffsetMillis)
        TorchControllerResult.Ok
    }

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchControllerResult = runGated(RootFeatureKey.TorchMultiLed) {
        multiLed.activate(durationMillis, includeScreen)
        TorchControllerResult.Ok
    }

    override suspend fun withThermalOverride(
        durationMillis: Long,
        block: suspend () -> Unit,
    ): TorchControllerResult = runGated(RootFeatureKey.TorchThermalOverride) {
        thermal.withOverride(durationMillis, block)
    }

    private suspend fun writeBoostedBrightness(percent: Int): TorchControllerResult {
        val node = paths.resolvePrimary() ?: return TorchControllerResult.Unsupported
        val maxResult = shell.exec("cat \"${node.maxBrightnessPath}\"")
        val max = maxResult.stdout.firstOrNull()?.trim()?.toIntOrNull()
            ?: return TorchControllerResult.HardwareError(
                "Could not read max_brightness for ${node.label}",
            )
        val boostCeiling = (max.toLong() * BRIGHTNESS_BOOST_CAP_PERCENT) / PERCENT_DENOMINATOR
        val target = (max.toLong() * percent / PERCENT_DENOMINATOR).coerceIn(0L, boostCeiling)
        val write = shell.exec("echo $target > \"${node.brightnessPath}\"")
        return if (write.isSuccess) {
            TorchControllerResult.Ok
        } else {
            val stderr = write.stderr.firstOrNull().orEmpty()
            TorchControllerResult.HardwareError("brightness write failed: $stderr")
        }
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> TorchControllerResult,
    ): TorchControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is TorchControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> TorchControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            TorchControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> TorchControllerResult.Unsupported
    }
}
