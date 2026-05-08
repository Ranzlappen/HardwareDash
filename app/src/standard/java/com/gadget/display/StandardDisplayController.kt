package com.gadget.display

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Display controller. Every privileged method returns
 * [DisplayControllerResult.Unsupported] — there is no privileged shell
 * in this APK so direct backlight sysfs writes, `cmd display`, and
 * `wm density` are physically impossible.
 */
@Singleton
class StandardDisplayController @Inject constructor() : DisplayController {

    override suspend fun overrideBrightness(config: BrightnessOverrideConfig): DisplayControllerResult =
        DisplayControllerResult.Unsupported

    override suspend fun overrideRefreshRate(config: RefreshRateOverrideConfig): DisplayControllerResult =
        DisplayControllerResult.Unsupported

    override suspend fun overrideDensity(config: DensityOverrideConfig): DisplayControllerResult =
        DisplayControllerResult.Unsupported

    override suspend fun surfaceFlingerSnapshot(): DisplayControllerResult =
        DisplayControllerResult.Unsupported

    override suspend fun resetAllDisplayMutations(): DisplayControllerResult =
        DisplayControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): DisplayControllerResult =
        DisplayControllerResult.ResetCompleted(restored = 0, failed = 0)
}
