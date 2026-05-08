package com.gadget.battery

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

internal const val THERMAL_WATCHER_POLL_INTERVAL_MILLIS = 500L

/**
 * Polls every charger / battery / USB thermal zone and surfaces the first
 * zone whose current `temp` breaches its `trip_point_0_temp`. The
 * dangerous-tier helpers ([ChargingProfileOverride], [ThermalThrottleBypass])
 * launch this in a sibling coroutine and trip a shared abort flag when
 * the watcher returns a non-null breach.
 *
 * The helper polls every 500 ms — fast enough to react to a runaway
 * charge cycle within a single hard-ceiling window, slow enough to
 * keep the privileged shell quiet.
 */
@Singleton
class ThermalZoneWatcher @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
) {
    suspend fun pollUntilBreachOrCeiling(
        ceilingMillis: Long,
    ): ThermalBreachReport? {
        val zones = psuSysfs.listThermalZones()
            .map { psuSysfs.readThermalZone(it) }
            .filter { isMonitoredZone(it) }
        val zonesWithTrips = zones.mapNotNull { zone ->
            val trip = zone.tripPoints["trip_point_0_temp"]?.toLongOrNull() ?: return@mapNotNull null
            zone.zoneDir to trip
        }
        if (zonesWithTrips.isEmpty()) {
            // No usable trip-point info — fall back to a flat duration wait.
            delay(ceilingMillis)
            return null
        }

        val deadlineMs = System.currentTimeMillis() + ceilingMillis
        while (coroutineContext.isActive && System.currentTimeMillis() < deadlineMs) {
            for ((zoneDir, trip) in zonesWithTrips) {
                val tempStr = psuSysfs.readNode("${zoneDir}temp") ?: continue
                val temp = tempStr.toLongOrNull() ?: continue
                if (temp >= trip) {
                    return ThermalBreachReport(
                        zoneDir = zoneDir,
                        observedTemp = temp,
                        tripPoint = trip,
                    )
                }
            }
            delay(THERMAL_WATCHER_POLL_INTERVAL_MILLIS)
        }
        return null
    }

    private fun isMonitoredZone(zone: ThermalZoneSnapshot): Boolean {
        val type = zone.type?.lowercase().orEmpty()
        return type.contains("charger") ||
            type.contains("battery") ||
            type.contains("batt") ||
            type.contains("usb") ||
            type.contains("pmic")
    }
}

data class ThermalBreachReport(
    val zoneDir: String,
    val observedTemp: Long,
    val tripPoint: Long,
)
