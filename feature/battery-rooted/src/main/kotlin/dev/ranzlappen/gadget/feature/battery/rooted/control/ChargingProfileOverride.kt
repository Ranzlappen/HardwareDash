package dev.ranzlappen.gadget.feature.battery.rooted.control

import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.battery.control.BatteryControllerResult
import dev.ranzlappen.gadget.feature.battery.control.ChargingProfileConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal const val CHARGING_PROFILE_HARD_CEILING_MILLIS = 30_000L
private const val CHARGE_CURRENT_NODE = "/sys/class/power_supply/battery/constant_charge_current_max"
private const val CHARGE_VOLTAGE_NODE = "/sys/class/power_supply/battery/constant_charge_voltage_max"

/**
 * Writes `constant_charge_current_max` / `constant_charge_voltage_max`
 * to override OEM charging caps. **EXTREMELY DANGEROUS** — pushing
 * beyond OEM-validated limits can cause thermal runaway, swelling, or
 * fire. Hard 30-second active window, thermal-zone-monitor coroutine
 * that aborts + restores immediately on any zone breaching its
 * `trip_point_0_temp`.
 *
 * The original values are snapshotted before write and restored in a
 * `NonCancellable` finally regardless of which exit path runs (timeout,
 * thermal abort, cancellation, exception).
 */
@Singleton
class ChargingProfileOverride @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
    private val mutationLog: SysfsMutationLog,
    private val thermalWatcher: ThermalZoneWatcher,
) {
    suspend fun apply(config: ChargingProfileConfig): BatteryControllerResult {
        val originalCurrent = if (config.maxCurrentMicroAmps != null) {
            psuSysfs.readNode(CHARGE_CURRENT_NODE)
        } else null
        val originalVoltage = if (config.maxVoltageMicroVolts != null) {
            psuSysfs.readNode(CHARGE_VOLTAGE_NODE)
        } else null

        if (config.maxCurrentMicroAmps != null && originalCurrent == null) {
            return BatteryControllerResult.Unsupported
        }
        if (config.maxVoltageMicroVolts != null && originalVoltage == null) {
            return BatteryControllerResult.Unsupported
        }
        if (originalCurrent == null && originalVoltage == null) {
            return BatteryControllerResult.HardwareError("nothing to write")
        }

        val effectiveDuration = config.durationMillis.coerceAtMost(CHARGING_PROFILE_HARD_CEILING_MILLIS)
        if (originalCurrent != null) mutationLog.register(CHARGE_CURRENT_NODE, originalCurrent)
        if (originalVoltage != null) mutationLog.register(CHARGE_VOLTAGE_NODE, originalVoltage)

        return try {
            if (config.maxCurrentMicroAmps != null) {
                val ok = psuSysfs.writeNode(CHARGE_CURRENT_NODE, config.maxCurrentMicroAmps.toString())
                if (!ok) return BatteryControllerResult.HardwareError(
                    "kernel rejected write to $CHARGE_CURRENT_NODE",
                )
            }
            if (config.maxVoltageMicroVolts != null) {
                val ok = psuSysfs.writeNode(CHARGE_VOLTAGE_NODE, config.maxVoltageMicroVolts.toString())
                if (!ok) return BatteryControllerResult.HardwareError(
                    "kernel rejected write to $CHARGE_VOLTAGE_NODE",
                )
            }
            val breach = thermalWatcher.pollUntilBreachOrCeiling(effectiveDuration)
            if (breach != null) {
                BatteryControllerResult.DangerousAborted(
                    reason = "thermal trip @ ${breach.zoneDir} " +
                        "(${breach.observedTemp} ≥ ${breach.tripPoint})",
                )
            } else {
                BatteryControllerResult.Ok()
            }
        } finally {
            withContext(NonCancellable) {
                if (originalCurrent != null) {
                    if (psuSysfs.writeNode(CHARGE_CURRENT_NODE, originalCurrent)) {
                        mutationLog.unregister(CHARGE_CURRENT_NODE)
                    }
                }
                if (originalVoltage != null) {
                    if (psuSysfs.writeNode(CHARGE_VOLTAGE_NODE, originalVoltage)) {
                        mutationLog.unregister(CHARGE_VOLTAGE_NODE)
                    }
                }
            }
        }
    }
}
