package com.gadget.torch

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Torch controller. Every extreme-tier method returns
 * [TorchControllerResult.Unsupported] — the standard APK has no privileged
 * shell, so there is no way to write to `/sys/class/leds/...` no matter how
 * hard the user tries.
 *
 * Shared UI checks the result and hides the corresponding control. Compose
 * code never branches on `BuildConfig.IS_ROOTED` — it just asks the
 * controller and trusts the answer.
 */
@Singleton
class StandardTorchController @Inject constructor() : TorchController {

    override suspend fun boostBrightness(percent: Int): TorchControllerResult =
        TorchControllerResult.Unsupported

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
        phaseOffsetMillis: Long,
    ): TorchControllerResult = TorchControllerResult.Unsupported

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchControllerResult = TorchControllerResult.Unsupported

    override suspend fun withThermalOverride(
        durationMillis: Long,
        block: suspend () -> Unit,
    ): TorchControllerResult = TorchControllerResult.Unsupported
}
