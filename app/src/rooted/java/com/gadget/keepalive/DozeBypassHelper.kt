package com.gadget.keepalive

import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `cmd deviceidle whitelist` wrapper. Always scoped to the app's own
 * package — passing a foreign package via [DozeBypassConfig] is
 * accepted but ignored; the helper substitutes the own pkg.
 *
 * Mutation registered with the shared log under the synthesized
 * `cmd-deviceidle://whitelist/<pkg>` pseudo-path so revert works.
 */
@Singleton
class DozeBypassHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun whitelist(packageName: String): KeepAliveControllerResult {
        val pseudoPath = "cmd-deviceidle://whitelist/$packageName"
        mutationLog.register(pseudoPath, "absent")
        val result = shell.exec("cmd deviceidle whitelist +\"$packageName\"")
        if (!result.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return KeepAliveControllerResult.HardwareError(
                "cmd deviceidle whitelist rejected: exit=${result.exitCode}",
            )
        }
        return KeepAliveControllerResult.Ok(statusNote = "Whitelisted $packageName")
    }
}
