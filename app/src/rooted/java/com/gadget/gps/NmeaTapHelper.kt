package com.gadget.gps

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val GPS_NMEA_HARD_CEILING_MILLIS = 30_000L
private const val SHELL_TIMEOUT_MARGIN_MILLIS = 2_000L
private val NMEA_NODE_CANDIDATES = listOf(
    "/sys/class/gnss/gnss0/nmea",
    "/dev/ttyHSL0",
    "/dev/gnss0",
    "/dev/ttyACM0",
)

/**
 * Read-only `cat` of vendor NMEA nodes for a bounded window. Hard
 * 30-second ceiling regardless of caller input.
 */
@Singleton
class NmeaTapHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun tap(durationMillis: Long): List<String>? {
        val effectiveMillis = durationMillis.coerceAtMost(GPS_NMEA_HARD_CEILING_MILLIS)
        val seconds = (effectiveMillis / 1000).coerceAtLeast(1L)
        val node = NMEA_NODE_CANDIDATES.firstOrNull { isReadable(it) } ?: return null
        val script = "timeout $seconds cat \"$node\" 2>/dev/null | head -n 200"
        val result = shell.exec(script, timeoutMillis = effectiveMillis + SHELL_TIMEOUT_MARGIN_MILLIS)
        if (!result.isSuccess && result.stdout.isEmpty()) return null
        return result.stdout
    }

    private suspend fun isReadable(path: String): Boolean {
        val probe = shell.exec("test -r \"$path\" && echo ok")
        return probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok"
    }
}
