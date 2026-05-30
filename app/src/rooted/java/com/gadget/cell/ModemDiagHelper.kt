package com.gadget.cell

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private val MODEM_DIAG_GLOBS = listOf(
    "ls -1d /sys/class/qcom_smd* 2>/dev/null",
    "ls -1 /proc/qmi_devices 2>/dev/null",
    "ls -1d /sys/class/net/rmnet*/ 2>/dev/null",
)
private val SIGNAL_DEEP_NODE_CANDIDATES = listOf(
    "/sys/class/net/rmnet0/statistics",
    "/sys/devices/virtual/cellular/cellular0/signal_strength",
    "/proc/net/wireless",
)

/**
 * Read-only modem-diagnostic dump. Walks the standard Qualcomm-style
 * sysfs locations; surfaces an empty map on non-Qualcomm SoCs (which
 * the caller treats as `Unsupported`).
 */
@Singleton
class ModemDiagHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun dumpModem(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (glob in MODEM_DIAG_GLOBS) {
            val ls = shell.exec(glob)
            if (!ls.isSuccess) continue
            for (path in ls.stdout.flatMap { it.trim().split(Regex("\\s+")) }) {
                if (path.isEmpty()) continue
                val nameLs = shell.exec("ls -1 \"$path\" 2>/dev/null")
                if (!nameLs.isSuccess) {
                    out[path] = "(no children)"
                    continue
                }
                for (entry in nameLs.stdout.flatMap { it.trim().split(Regex("\\s+")) }) {
                    val full = "$path/$entry"
                    val read = shell.exec("cat \"$full\" 2>/dev/null")
                    if (read.isSuccess) {
                        out[full] = read.stdout.firstOrNull()?.trim().orEmpty()
                    }
                }
            }
        }
        return out
    }

    suspend fun dumpSignalDeep(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (path in SIGNAL_DEEP_NODE_CANDIDATES) {
            val read = shell.exec("cat \"$path\" 2>/dev/null")
            if (read.isSuccess && read.stdout.isNotEmpty()) {
                out[path] = read.stdout.joinToString(" / ")
            }
        }
        return out
    }
}
