package com.gadget.sensors

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only walk over every sensor-adjacent kernel surface: IIO devices,
 * the legacy `/sys/class/sensors/` tree, `/proc/bus/input/devices`, and
 * `/dev/input/event*`. Used by `SensorsController.enumerateHidden` to
 * surface sensors the framework hides (hall sensors, lid switches,
 * vendor-proprietary IMUs).
 */
@Singleton
class HiddenSensorEnumerator @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun enumerate(): List<String> {
        val nodes = mutableListOf<String>()
        nodes += listing("ls -1d /sys/bus/iio/devices/iio:device* 2>/dev/null")
        nodes += listing("ls -1d /sys/class/sensors/* 2>/dev/null")
        nodes += listing("ls -1 /dev/input/event* 2>/dev/null")
        nodes += parseInputDevices()
        return nodes.distinct().filter { it.isNotEmpty() }
    }

    private suspend fun listing(command: String): List<String> {
        val result = shell.exec(command)
        if (!result.isSuccess) return emptyList()
        return result.stdout.flatMap { it.trim().split(Regex("\\s+")) }.filter { it.isNotEmpty() }
    }

    private suspend fun parseInputDevices(): List<String> {
        val result = shell.exec("cat /proc/bus/input/devices 2>/dev/null")
        if (!result.isSuccess) return emptyList()
        return result.stdout.mapNotNull { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("N: Name=") -> "input:${trimmed.removePrefix("N: Name=").trim('"')}"
                else -> null
            }
        }
    }
}
