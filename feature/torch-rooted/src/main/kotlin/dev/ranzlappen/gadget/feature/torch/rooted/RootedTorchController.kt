package dev.ranzlappen.gadget.feature.torch.rooted

import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsController
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsControllerResult
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
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
) : TorchSysfsController {

    override suspend fun boostBrightness(percent: Int): TorchSysfsControllerResult =
        runGated(RootFeatureKey.TorchExtremeBrightness) {
            writeBoostedBrightness(percent)
        }

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long,
    ): TorchSysfsControllerResult = runGated(RootFeatureKey.TorchHighFrequencyStrobe) {
        val node = paths.resolvePrimary() ?: return@runGated TorchSysfsControllerResult.Unsupported
        strobe.run(node, frequencyHz, dutyPercent, durationMillis, phaseOffsetMillis)
        TorchSysfsControllerResult.Ok
    }

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchSysfsControllerResult = runGated(RootFeatureKey.TorchMultiLed) {
        multiLed.activate(durationMillis, includeScreen)
        TorchSysfsControllerResult.Ok
    }

    override suspend fun withThermalOverride(
        durationMillis: Long,
        block: suspend () -> Unit,
    ): TorchSysfsControllerResult = runGated(RootFeatureKey.TorchThermalOverride) {
        thermal.withOverride(durationMillis, block)
    }

    private suspend fun writeBoostedBrightness(percent: Int): TorchSysfsControllerResult {
        val node = paths.resolvePrimary() ?: return TorchSysfsControllerResult.Unsupported
        val maxResult = shell.exec("cat \"${node.maxBrightnessPath}\"")
        val max = maxResult.stdout.firstOrNull()?.trim()?.toIntOrNull()
            ?: return TorchSysfsControllerResult.HardwareError(
                "Could not read max_brightness for ${node.label}",
            )
        val boostCeiling = (max.toLong() * BRIGHTNESS_BOOST_CAP_PERCENT) / PERCENT_DENOMINATOR
        val target = (max.toLong() * percent / PERCENT_DENOMINATOR).coerceIn(0L, boostCeiling)
        val write = shell.exec("echo $target > \"${node.brightnessPath}\"")
        return if (write.isSuccess) {
            TorchSysfsControllerResult.Ok
        } else {
            val stderr = write.stderr.firstOrNull().orEmpty()
            TorchSysfsControllerResult.HardwareError("brightness write failed: $stderr")
        }
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> TorchSysfsControllerResult,
    ): TorchSysfsControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is TorchSysfsControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> TorchSysfsControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            TorchSysfsControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> TorchSysfsControllerResult.Unsupported
    }
}
