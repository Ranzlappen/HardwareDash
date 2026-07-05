package dev.ranzlappen.gadget.feature.keepalive.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.keepalive.control.KeepAliveControllerResult
import dev.ranzlappen.gadget.feature.keepalive.control.PmGrantConfig
import dev.ranzlappen.gadget.feature.keepalive.control.PmGrantVerb
import javax.inject.Inject
import javax.inject.Singleton

private val GRANT_ALLOW_LIST = setOf(
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.POST_NOTIFICATIONS",
)

/**
 * `pm grant` / `pm revoke` wrapped against a hard allow-list of
 * normal-protection-level permissions. Anything outside the allow-list
 * is rejected regardless of caller input.
 */
@Singleton
class PmGrantHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun grant(
        packageName: String,
        config: PmGrantConfig,
    ): KeepAliveControllerResult {
        val rejected = config.permissions.filterNot { it in GRANT_ALLOW_LIST }
        if (rejected.isNotEmpty()) {
            return KeepAliveControllerResult.HardwareError(
                "permissions outside allow-list: ${rejected.joinToString()}",
            )
        }
        val verb = if (config.grantOrRevoke == PmGrantVerb.GRANT) "grant" else "revoke"
        var ok = 0
        var fail = 0
        for (perm in config.permissions) {
            val pseudoPath = "pm-grant://$packageName/$perm"
            if (config.grantOrRevoke == PmGrantVerb.GRANT) {
                mutationLog.register(pseudoPath, "denied")
            }
            val result = shell.exec("pm $verb \"$packageName\" \"$perm\"")
            if (result.isSuccess) {
                ok++
                if (config.grantOrRevoke == PmGrantVerb.REVOKE) {
                    mutationLog.unregister(pseudoPath)
                }
            } else {
                fail++
                if (config.grantOrRevoke == PmGrantVerb.GRANT) {
                    mutationLog.unregister(pseudoPath)
                }
            }
        }
        return if (fail == 0) {
            KeepAliveControllerResult.Ok(statusNote = "$verb succeeded for $ok permission(s)")
        } else {
            KeepAliveControllerResult.HardwareError("$verb: $ok ok, $fail failed")
        }
    }
}
