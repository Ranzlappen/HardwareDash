package com.gadget.adbdebug

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val ADB_NETWORK_PSEUDO_PATH = "setprop://service.adb.tcp.port"
internal const val ADB_NETWORK_PORT_MIN = 5555
internal const val ADB_NETWORK_PORT_MAX = 5599
internal const val ADB_NETWORK_PORT_DISABLED = -1

private const val ADB_NETWORK_PROP = "service.adb.tcp.port"

/**
 * Toggles ADB-over-network. Allowed ports: 5555–5599. The helper:
 *
 * 1. Snapshots the prior `service.adb.tcp.port` value via `getprop`.
 * 2. Registers the pre-mutation value with [SysfsMutationLog] under
 *    `setprop://service.adb.tcp.port` so process kill is not a stuck
 *    state.
 * 3. Writes the new port (or `-1` to disable) via `setprop`.
 * 4. Restarts adbd via `setprop ctl.restart adbd` (canonical form on
 *    init.rc-based services through API 35).
 */
@Singleton
class AdbNetworkHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun apply(config: AdbNetworkConfig): AdbDebuggingControllerResult {
        if (config.enabled && config.port !in ADB_NETWORK_PORT_MIN..ADB_NETWORK_PORT_MAX) {
            return AdbDebuggingControllerResult.HardwareError(
                "port ${config.port} outside allow-list ${ADB_NETWORK_PORT_MIN}-$ADB_NETWORK_PORT_MAX",
            )
        }
        val priorRaw = readPort()
        val priorPort = priorRaw?.toIntOrNull()
        val targetPort = if (config.enabled) config.port else ADB_NETWORK_PORT_DISABLED
        mutationLog.register(ADB_NETWORK_PSEUDO_PATH, priorRaw ?: "")
        val setpropResult = shell.exec("setprop $ADB_NETWORK_PROP $targetPort")
        if (!setpropResult.isSuccess) {
            mutationLog.unregister(ADB_NETWORK_PSEUDO_PATH)
            return AdbDebuggingControllerResult.HardwareError(
                "setprop $ADB_NETWORK_PROP $targetPort failed (exit=${setpropResult.exitCode})",
            )
        }
        val restartResult = shell.exec("setprop ctl.restart adbd")
        if (!restartResult.isSuccess) {
            return AdbDebuggingControllerResult.HardwareError(
                "ctl.restart adbd failed (exit=${restartResult.exitCode})",
            )
        }
        return AdbDebuggingControllerResult.AdbNetworkSnapshot(
            appliedPort = targetPort.takeIf { it > 0 },
            priorPort = priorPort?.takeIf { it > 0 },
        )
    }

    private suspend fun readPort(): String? {
        val result = shell.exec("getprop $ADB_NETWORK_PROP")
        if (!result.isSuccess) return null
        val raw = result.stdout.firstOrNull()?.trim() ?: return null
        return raw.ifEmpty { null }
    }
}
