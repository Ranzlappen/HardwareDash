package dev.ranzlappen.gadget.feature.battery.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val POWER_SUPPLY_GLOB = "ls -d /sys/class/power_supply/*/ 2>/dev/null"
private const val THERMAL_ZONE_GLOB = "ls -d /sys/class/thermal/thermal_zone*/ 2>/dev/null"

/**
 * Probes `/sys/class/power_supply/...` and `/sys/class/thermal/thermal_zone*`
 * via the privileged shell. Surfaces every readable node as a flat
 * key/value map for diagnostic dumps and provides utility methods for
 * targeted reads / writes used by the dangerous-tier helpers in 5e.
 */
@Singleton
class PowerSupplySysfs @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun listPsuDirs(): List<String> = listing(POWER_SUPPLY_GLOB)

    suspend fun listThermalZones(): List<String> = listing(THERMAL_ZONE_GLOB)

    suspend fun readNode(path: String): String? {
        val result = shell.exec("cat \"$path\" 2>/dev/null")
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()
    }

    suspend fun writeNode(path: String, value: String): Boolean =
        shell.exec("echo \"$value\" > \"$path\"").isSuccess

    suspend fun readPsuMap(psuDir: String): Map<String, String> {
        val ls = shell.exec("ls -1 \"$psuDir\" 2>/dev/null")
        if (!ls.isSuccess) return emptyMap()
        val nodes = ls.stdout.flatMap { it.trim().split(Regex("\\s+")) }.filter { it.isNotEmpty() }
        val out = LinkedHashMap<String, String>()
        for (node in nodes) {
            val full = "$psuDir$node"
            val value = readNode(full) ?: continue
            out[node] = value
        }
        return out
    }

    suspend fun readThermalZone(zoneDir: String): ThermalZoneSnapshot {
        val type = readNode("${zoneDir}type")
        val temp = readNode("${zoneDir}temp")
        val mode = readNode("${zoneDir}mode")
        val tripPoints = mutableMapOf<String, String>()
        val ls = shell.exec("ls -1 \"$zoneDir\" 2>/dev/null")
        if (ls.isSuccess) {
            for (entry in ls.stdout.flatMap { it.trim().split(Regex("\\s+")) }) {
                if (entry.startsWith("trip_point_") && entry.endsWith("_temp")) {
                    val v = readNode("$zoneDir$entry") ?: continue
                    tripPoints[entry] = v
                }
            }
        }
        return ThermalZoneSnapshot(
            zoneDir = zoneDir,
            type = type,
            temp = temp,
            mode = mode,
            tripPoints = tripPoints,
        )
    }

    private suspend fun listing(command: String): List<String> {
        val result = shell.exec(command)
        if (!result.isSuccess) return emptyList()
        return result.stdout.flatMap { it.trim().split(Regex("\\s+")) }.filter { it.isNotEmpty() }
    }
}

data class ThermalZoneSnapshot(
    val zoneDir: String,
    val type: String?,
    val temp: String?,
    val mode: String?,
    val tripPoints: Map<String, String>,
)
