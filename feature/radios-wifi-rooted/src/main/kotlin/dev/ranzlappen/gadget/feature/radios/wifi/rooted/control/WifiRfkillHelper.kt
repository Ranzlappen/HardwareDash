package dev.ranzlappen.gadget.feature.radios.wifi.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val RFKILL_BIN = "rfkill"
private const val RFKILL_LIST_FORMAT = "rfkill list wifi"
private const val RFKILL_BLOCK_FORMAT = "rfkill block wifi"
private const val RFKILL_UNBLOCK_FORMAT = "rfkill unblock wifi"

/**
 * `rfkill` shell-out wrapper for the Wi-Fi radio. On stock AOSP the
 * binary is sometimes absent (~60 % of devices per the Batch-6
 * feasibility report) — the helper probes via `which` and surfaces
 * `Unsupported` cleanly when missing.
 */
@Singleton
class WifiRfkillHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun isAvailable(): Boolean {
        val probe = shell.exec("which $RFKILL_BIN")
        return probe.isSuccess && !probe.stdout.firstOrNull()?.trim().isNullOrEmpty()
    }

    suspend fun isCurrentlyBlocked(): Boolean? {
        val result = shell.exec(RFKILL_LIST_FORMAT)
        if (!result.isSuccess) return null
        return result.stdout
            .joinToString("\n")
            .lines()
            .firstOrNull { it.trim().startsWith("Soft blocked", ignoreCase = true) }
            ?.contains("yes", ignoreCase = true)
    }

    suspend fun setBlocked(blocked: Boolean): Boolean {
        val command = if (blocked) RFKILL_BLOCK_FORMAT else RFKILL_UNBLOCK_FORMAT
        return shell.exec(command).isSuccess
    }
}
