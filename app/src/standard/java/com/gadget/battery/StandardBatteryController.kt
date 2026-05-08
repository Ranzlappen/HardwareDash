package com.gadget.battery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Battery controller. Every extreme-tier method returns
 * [BatteryControllerResult.Unsupported] — the standard APK has no
 * privileged shell so direct `/sys/class/power_supply/...` writes and
 * thermal-zone overrides are impossible regardless of permissions.
 */
@Singleton
class StandardBatteryController @Inject constructor() : BatteryController {

    override suspend fun fuelGaugeRaw(): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun cellMonitor(): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun chargingProfile(config: ChargingProfileConfig): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun thermalBypass(config: ThermalBypassConfig): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun chargingTypeOverride(config: ChargingTypeOverrideConfig): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun fullDump(): BatteryControllerResult =
        BatteryControllerResult.Unsupported

    override suspend fun resetAllBatteryMutations(): BatteryControllerResult =
        BatteryControllerResult.ResetCompleted(restored = 0, failed = 0)
}
