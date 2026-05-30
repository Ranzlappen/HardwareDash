package com.gadget.adbdebug

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val ADB_ENABLED_PSEUDO_PATH = "adb-toggle://global/adb_enabled"

private const val ADB_ENABLED_KEY = "adb_enabled"

/**
 * Narrow ADB-enabled toggle helper. Owns its own one-key allow-list
 * (`{adb_enabled}`) and its own pseudo-path namespace (`adb-toggle://`)
 * so the Batch-7 [com.gadget.automation.SystemSettingsHelper] does not
 * need to be modified. Snapshot+restore via the mutation log so screen-exit
 * revert can flip the toggle back if the user navigates away.
 */
@Singleton
class AdbSettingsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun setEnabled(enabled: Boolean): AdbDebuggingControllerResult {
        val priorRaw = readCurrent()
        val priorEnabled = priorRaw?.toIntOrNull()?.let { it != 0 }
        val applied = if (enabled) "1" else "0"
        if (priorRaw != null) {
            mutationLog.register(ADB_ENABLED_PSEUDO_PATH, priorRaw)
        }
        val result = shell.exec("settings put global $ADB_ENABLED_KEY $applied")
        if (!result.isSuccess) {
            mutationLog.unregister(ADB_ENABLED_PSEUDO_PATH)
            return AdbDebuggingControllerResult.HardwareError(
                "settings put global $ADB_ENABLED_KEY $applied failed (exit=${result.exitCode})",
            )
        }
        return AdbDebuggingControllerResult.AdbToggleSnapshot(
            appliedEnabled = enabled,
            priorEnabled = priorEnabled,
        )
    }

    private suspend fun readCurrent(): String? {
        val result = shell.exec("settings get global $ADB_ENABLED_KEY 2>/dev/null")
        if (!result.isSuccess) return null
        val line = result.stdout.firstOrNull()?.trim() ?: return null
        if (line.isEmpty() || line == "null") return null
        return line
    }
}
