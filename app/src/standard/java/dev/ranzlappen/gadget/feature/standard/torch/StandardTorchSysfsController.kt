package dev.ranzlappen.gadget.feature.standard.torch

import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsController
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Torch sysfs controller. Every extreme-tier method returns
 * [TorchSysfsControllerResult.Unsupported] — the standard APK has no privileged
 * shell, so there is no way to write to `/sys/class/leds/...` no matter how
 * hard the user tries.
 *
 * Shared UI checks the result and hides the corresponding control. Compose
 * code never branches on `BuildConfig.IS_ROOTED` — it just asks the
 * controller and trusts the answer.
 */
@Singleton
class StandardTorchSysfsController @Inject constructor() : TorchSysfsController {

    override suspend fun boostBrightness(percent: Int): TorchSysfsControllerResult =
        TorchSysfsControllerResult.Unsupported

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long,
    ): TorchSysfsControllerResult = TorchSysfsControllerResult.Unsupported

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchSysfsControllerResult = TorchSysfsControllerResult.Unsupported

    override suspend fun withThermalOverride(
        durationMillis: Long,
        block: suspend () -> Unit,
    ): TorchSysfsControllerResult = TorchSysfsControllerResult.Unsupported
}
