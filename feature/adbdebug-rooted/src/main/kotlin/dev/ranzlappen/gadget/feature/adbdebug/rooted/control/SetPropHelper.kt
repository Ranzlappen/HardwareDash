package dev.ranzlappen.gadget.feature.adbdebug.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.adbdebug.control.SetPropConfig
import javax.inject.Inject
import javax.inject.Singleton

internal const val SETPROP_PSEUDO_PATH_PREFIX = "setprop://"

private val SETPROP_EXACT_ALLOW_LIST = setOf(
    "debug.hwui.renderer",
    "debug.hwui.profile",
    "debug.egl.profiler",
    "dalvik.vm.heapsize",
    "dalvik.vm.heapgrowthlimit",
    "persist.adb.tcp.port",
    "persist.sys.usb.config",
)
private val SETPROP_PREFIX_ALLOW_LIST = listOf(
    "log.tag.",
)
private val SETPROP_FORBIDDEN_PREFIXES = listOf(
    "ro.",
)

/**
 * Allow-listed `setprop` override. The helper enforces the allow-list
 * regardless of caller. Anything starting with `ro.` is refused — those
 * properties are read-only at the kernel level on stock Android, and
 * pretending otherwise would silently mislead the caller.
 */
@Singleton
class SetPropHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun apply(config: SetPropConfig): AdbDebuggingControllerResult {
        val key = config.key.trim()
        if (key.isEmpty()) {
            return AdbDebuggingControllerResult.HardwareError("empty property key")
        }
        if (SETPROP_FORBIDDEN_PREFIXES.any { key.startsWith(it) }) {
            return AdbDebuggingControllerResult.HardwareError(
                "$key is read-only at the kernel level",
            )
        }
        val allowedExact = key in SETPROP_EXACT_ALLOW_LIST
        val allowedPrefix = SETPROP_PREFIX_ALLOW_LIST.any { key.startsWith(it) }
        if (!allowedExact && !allowedPrefix) {
            return AdbDebuggingControllerResult.HardwareError(
                "$key not in setprop allow-list",
            )
        }
        val priorRaw = readProp(key)
        val pseudoPath = "$SETPROP_PSEUDO_PATH_PREFIX$key"
        mutationLog.register(pseudoPath, priorRaw ?: "")
        val result = shell.exec("setprop ${shellQuote(key)} ${shellQuote(config.value)}")
        if (!result.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return AdbDebuggingControllerResult.HardwareError(
                "setprop $key failed (exit=${result.exitCode})",
            )
        }
        return AdbDebuggingControllerResult.SetpropSnapshot(
            key = key,
            appliedValue = config.value,
            priorValue = priorRaw,
        )
    }

    private suspend fun readProp(key: String): String? {
        val result = shell.exec("getprop ${shellQuote(key)}")
        if (!result.isSuccess) return null
        val raw = result.stdout.firstOrNull()?.trim() ?: return null
        return raw.ifEmpty { null }
    }

    private fun shellQuote(value: String): String {
        val escaped = value.replace("'", "'\\''")
        return "'$escaped'"
    }
}
