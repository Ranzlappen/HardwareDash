package com.gadget.wifi

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val IW_PHY_INFO_COMMAND = "iw phy phy0 info"
private const val EXCERPT_LINE_LIMIT = 12

/**
 * **Read-only** capability probe. Inspects `iw phy <phy> info` output
 * for the strings the kernel emits when the driver advertises monitor
 * mode / IBSS — does NOT enable injection. Actual packet injection
 * requires a custom kernel module (e.g. nexmon) which this app does
 * not ship.
 */
@Singleton
class WifiInjectionProbe @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun probe(): WifiControllerResult.InjectionCapabilityProbe? {
        val result = shell.exec(IW_PHY_INFO_COMMAND)
        if (!result.isSuccess) return null
        val joined = result.stdout.joinToString("\n")
        val supportsMonitor = joined.contains("* monitor", ignoreCase = true)
        val supportsIbss = joined.contains("* IBSS", ignoreCase = true)
        val excerpt = result.stdout
            .filter { line ->
                val l = line.lowercase()
                l.contains("monitor") || l.contains("ibss") || l.contains("supported interface modes")
            }
            .take(EXCERPT_LINE_LIMIT)
            .joinToString("\n")
        return WifiControllerResult.InjectionCapabilityProbe(
            supportsMonitor = supportsMonitor,
            supportsIbss = supportsIbss,
            rawPhyInfoExcerpt = excerpt,
        )
    }
}
