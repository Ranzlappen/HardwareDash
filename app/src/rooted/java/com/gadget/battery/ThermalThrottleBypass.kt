package com.gadget.battery

import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val THERMAL_BYPASS_HARD_CEILING_MILLIS = 60_000L
private const val THERMAL_DISABLED_VALUE = "disabled"
private const val THERMAL_ENABLED_VALUE = "enabled"

/**
 * Writes `disabled` to charger / battery / USB thermal-zone `mode`
 * nodes. **Same severity as charging-profile override**. Hard 60-second
 * active window. Snapshot+restore in `NonCancellable` finally.
 *
 * The same thermal-zone monitor that backs `ChargingProfileOverride`
 * polls in parallel — if any matching zone breaches its
 * `trip_point_0_temp` even with throttling disabled, the helper aborts
 * and restores every mode value before returning.
 */
@Singleton
class ThermalThrottleBypass @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
    private val mutationLog: SysfsMutationLog,
    private val thermalWatcher: ThermalZoneWatcher,
) {
    suspend fun apply(config: ThermalBypassConfig): BatteryControllerResult {
        val targets = locateTargets()
        if (targets.isEmpty()) return BatteryControllerResult.Unsupported

        val effectiveDuration = config.durationMillis.coerceAtMost(THERMAL_BYPASS_HARD_CEILING_MILLIS)
        val snapshots = mutableListOf<ZoneModeSnapshot>()
        for (target in targets) {
            val original = psuSysfs.readNode(target.modePath) ?: continue
            mutationLog.register(target.modePath, original)
            val ok = psuSysfs.writeNode(target.modePath, THERMAL_DISABLED_VALUE)
            if (!ok) {
                mutationLog.unregister(target.modePath)
                continue
            }
            snapshots += ZoneModeSnapshot(target.modePath, original)
        }
        if (snapshots.isEmpty()) {
            return BatteryControllerResult.HardwareError("no thermal mode node accepted the write")
        }

        return try {
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
                for (snap in snapshots) {
                    val ok = psuSysfs.writeNode(snap.modePath, snap.originalValue)
                    if (ok) mutationLog.unregister(snap.modePath)
                    else psuSysfs.writeNode(snap.modePath, THERMAL_ENABLED_VALUE)
                }
            }
        }
    }

    private suspend fun locateTargets(): List<ZoneTarget> {
        val zones = psuSysfs.listThermalZones().map { psuSysfs.readThermalZone(it) }
        return zones
            .filter { isMonitoredZone(it) && it.mode != null }
            .map { ZoneTarget(modePath = "${it.zoneDir}mode") }
    }

    private fun isMonitoredZone(zone: ThermalZoneSnapshot): Boolean {
        val type = zone.type?.lowercase().orEmpty()
        return type.contains("charger") ||
            type.contains("battery") ||
            type.contains("batt") ||
            type.contains("usb") ||
            type.contains("pmic")
    }

    private data class ZoneTarget(val modePath: String)
    private data class ZoneModeSnapshot(val modePath: String, val originalValue: String)
}
