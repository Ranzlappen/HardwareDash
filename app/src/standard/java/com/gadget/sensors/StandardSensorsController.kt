package com.gadget.sensors

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Sensors controller. Every extreme-tier method returns
 * [SensorsControllerResult.Unsupported] — the standard APK has no
 * privileged shell so direct IIO / `/sys/class/sensors/` writes are
 * impossible regardless of permissions.
 */
@Singleton
class StandardSensorsController @Inject constructor() : SensorsController {

    override suspend fun highPolling(config: HighPollingConfig): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun rawUnfiltered(config: RawUnfilteredConfig): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun readSysfs(): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun overclock(config: OverclockConfig): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun fusionOverride(config: FusionOverrideConfig): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun enumerateHidden(): SensorsControllerResult =
        SensorsControllerResult.Unsupported

    override suspend fun resetAllSensorMutations(): SensorsControllerResult =
        SensorsControllerResult.ResetCompleted(restored = 0, failed = 0)
}
