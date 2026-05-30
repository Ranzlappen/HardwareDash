package com.gadget.gps

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private val CONSTELLATION_NODE_CANDIDATES = listOf(
    "/sys/class/gnss/gnss0/satellites",
    "/sys/class/sensors/gps/satellites",
    "/proc/gps/satellites",
)

/**
 * Best-effort enumeration of every visible satellite via vendor diag
 * nodes. Most stock Android devices do not expose a sysfs node for
 * this — the helper surfaces an empty list (caller treats as
 * `Unsupported`) on those devices.
 */
@Singleton
class ConstellationDumpHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun dump(): List<SatelliteEntry> {
        val node = CONSTELLATION_NODE_CANDIDATES.firstOrNull { isReadable(it) } ?: return emptyList()
        val read = shell.exec("cat \"$node\" 2>/dev/null")
        if (!read.isSuccess) return emptyList()
        return read.stdout.mapNotNull(::parseLine)
    }

    private fun parseLine(line: String): SatelliteEntry? {
        // Expected format (vendor-specific) but tolerant: whitespace-separated tokens
        // <constellation> <svId> <cn0> <elevation> <azimuth> <usedInFix>
        val tokens = line.trim().split(Regex("\\s+"))
        if (tokens.size < 2) return null
        val constellation = tokens[0]
        val svId = tokens[1].toIntOrNull() ?: return null
        val cn0 = tokens.getOrNull(2)?.toDoubleOrNull()
        val elevation = tokens.getOrNull(3)?.toDoubleOrNull()
        val azimuth = tokens.getOrNull(4)?.toDoubleOrNull()
        val used = tokens.getOrNull(5)?.lowercase() in setOf("1", "true", "yes", "y")
        return SatelliteEntry(
            constellation = constellation,
            svId = svId,
            cn0DbHz = cn0,
            elevationDegrees = elevation,
            azimuthDegrees = azimuth,
            usedInFix = used,
        )
    }

    private suspend fun isReadable(path: String): Boolean {
        val probe = shell.exec("test -r \"$path\" && echo ok")
        return probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok"
    }
}
