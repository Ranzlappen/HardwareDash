package com.gadget.sensors

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val SENSORS_HIGH_POLL_DEFAULT_HZ_CEILING = 400
internal const val SENSORS_HIGH_POLL_EXPERT_HZ_CEILING = 1_000
internal const val SENSORS_HIGH_POLL_HARD_CEILING_MILLIS = 60_000L
internal const val SENSORS_RAW_UNFILTERED_HARD_CEILING_MILLIS = 60_000L
private const val IIO_DEVICES_GLOB = "ls -d /sys/bus/iio/devices/iio:device* 2>/dev/null"
private const val LEGACY_SENSORS_GLOB = "ls -d /sys/class/sensors/* 2>/dev/null"

/**
 * Probes IIO + the legacy `/sys/class/sensors/` tree, drives sampling
 * frequency writes, and surfaces raw `_raw` / `_scale` / `_offset` triples
 * for the sysfs-read controller method. Every write registers the original
 * value with [SysfsMutationLog] before mutating so the global "Reset"
 * button can revert mutations the local finally missed (e.g. process
 * killed mid-call).
 */
@Singleton
class IioSensorsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {

    suspend fun listIioDevices(): List<String> {
        val result = shell.exec(IIO_DEVICES_GLOB)
        if (!result.isSuccess) return emptyList()
        return result.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .filter { it.startsWith("/sys/bus/iio/devices/iio:device") }
    }

    suspend fun listLegacySensorDirs(): List<String> {
        val result = shell.exec(LEGACY_SENSORS_GLOB)
        if (!result.isSuccess) return emptyList()
        return result.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .filter { it.startsWith("/sys/class/sensors/") }
    }

    suspend fun readSysfsTriples(devices: List<String>): List<SysfsReading> {
        val out = mutableListOf<SysfsReading>()
        for (device in devices) {
            val ls = shell.exec("ls -1 \"$device\" 2>/dev/null")
            if (!ls.isSuccess) continue
            val nodes = ls.stdout
                .flatMap { it.trim().split(Regex("\\s+")) }
                .filter { it.endsWith("_raw") }
            for (rawNode in nodes) {
                val rawPath = "$device/$rawNode"
                val rawValue = readNode(rawPath) ?: continue
                val baseName = rawNode.removeSuffix("_raw")
                val scalePath = "$device/${baseName}_scale"
                val offsetPath = "$device/${baseName}_offset"
                out += SysfsReading(
                    path = rawPath,
                    raw = rawValue,
                    scale = readNode(scalePath),
                    offset = readNode(offsetPath),
                )
            }
        }
        return out
    }

    /**
     * Writes [hz] to the matching IIO `sampling_frequency` node for the
     * named sensor. Snapshots the original value first (both into the
     * shared mutation log and into the returned [SamplingHandle] so the
     * caller can restore in their own `NonCancellable` finally).
     *
     * Returns `null` if the sensor's IIO node could not be located.
     */
    suspend fun setSamplingFrequency(sensorTag: String, hz: Int): SamplingHandle? {
        val nodePath = locateSamplingFrequencyNode(sensorTag) ?: return null
        val original = readNode(nodePath) ?: return null
        mutationLog.register(nodePath, original)
        val write = shell.exec("echo $hz > \"$nodePath\"")
        if (!write.isSuccess) {
            mutationLog.unregister(nodePath)
            return null
        }
        return SamplingHandle(nodePath = nodePath, originalValue = original)
    }

    /**
     * Restores the original value for the handle, both via the privileged
     * shell and by removing the entry from the mutation log on success.
     */
    suspend fun restoreSamplingFrequency(handle: SamplingHandle) {
        val command = "echo \"${handle.originalValue}\" > \"${handle.nodePath}\""
        val result = shell.exec(command)
        if (result.isSuccess) {
            mutationLog.unregister(handle.nodePath)
        }
    }

    /**
     * Best-effort: writes `0` to a filter cutoff node for the named
     * sensor where exposed (vendor-specific). Returns the original value
     * (for restore) or `null` if no filter node could be located.
     */
    suspend fun disableFilters(sensorTag: String): FilterHandle? {
        val nodePath = locateFilterCutoffNode(sensorTag) ?: return null
        val original = readNode(nodePath) ?: return null
        mutationLog.register(nodePath, original)
        val write = shell.exec("echo 0 > \"$nodePath\"")
        if (!write.isSuccess) {
            mutationLog.unregister(nodePath)
            return null
        }
        return FilterHandle(nodePath = nodePath, originalValue = original)
    }

    suspend fun restoreFilters(handle: FilterHandle) {
        val command = "echo \"${handle.originalValue}\" > \"${handle.nodePath}\""
        val result = shell.exec(command)
        if (result.isSuccess) {
            mutationLog.unregister(handle.nodePath)
        }
    }

    private suspend fun readNode(path: String): String? {
        val result = shell.exec("cat \"$path\" 2>/dev/null")
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()
    }

    private suspend fun locateSamplingFrequencyNode(sensorTag: String): String? {
        val devices = listIioDevices()
        for (device in devices) {
            val nameResult = shell.exec("cat \"$device/name\" 2>/dev/null")
            val name = nameResult.stdout.firstOrNull()?.trim().orEmpty()
            if (!name.contains(sensorTag, ignoreCase = true)) continue
            val candidates = listOf(
                "$device/sampling_frequency",
                "$device/in_${sensorTag.lowercase()}_sampling_frequency",
            )
            for (candidate in candidates) {
                val probe = shell.exec("test -w \"$candidate\" && echo ok")
                if (probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok") {
                    return candidate
                }
            }
        }
        return null
    }

    private suspend fun locateFilterCutoffNode(sensorTag: String): String? {
        val devices = listIioDevices()
        for (device in devices) {
            val nameResult = shell.exec("cat \"$device/name\" 2>/dev/null")
            val name = nameResult.stdout.firstOrNull()?.trim().orEmpty()
            if (!name.contains(sensorTag, ignoreCase = true)) continue
            val tagLower = sensorTag.lowercase()
            val candidates = listOf(
                "$device/in_${tagLower}_filter_low_pass_3db_frequency",
                "$device/in_${tagLower}_filter_high_pass_3db_frequency",
                "$device/filter_low_pass_3db_frequency",
            )
            for (candidate in candidates) {
                val probe = shell.exec("test -w \"$candidate\" && echo ok")
                if (probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok") {
                    return candidate
                }
            }
        }
        return null
    }
}

data class SamplingHandle(val nodePath: String, val originalValue: String)
data class FilterHandle(val nodePath: String, val originalValue: String)
