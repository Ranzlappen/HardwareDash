package com.gadget.automation

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val ALLOWED_KEYS = setOf(
    "screen_brightness",
    "screen_brightness_mode",
    "screen_off_timeout",
    "auto_time",
    "auto_time_zone",
    "accelerometer_rotation",
    "wifi_sleep_policy",
    "bluetooth_disabled_profiles",
    "notification_sound",
    "system_locales",
)

/**
 * Writes to `Settings.System` / `Secure` / `Global` via `settings put`.
 * Hard allow-list rejects every key not in [ALLOWED_KEYS] regardless of
 * caller. Snapshot+restore via the shared mutation log under the
 * synthesized `settings://<scope>/<key>` pseudo-path.
 */
@Singleton
class SystemSettingsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun put(config: SystemSettingsOverrideConfig): AutomationControllerResult {
        if (config.key !in ALLOWED_KEYS) {
            return AutomationControllerResult.HardwareError(
                "key ${config.key} is not in the writable allow-list",
            )
        }
        val scopeName = when (config.scope) {
            SystemSettingsScope.SYSTEM -> "system"
            SystemSettingsScope.SECURE -> "secure"
            SystemSettingsScope.GLOBAL -> "global"
        }
        val current = readCurrent(scopeName, config.key)
            ?: return AutomationControllerResult.HardwareError(
                "could not snapshot $scopeName/${config.key}",
            )
        val pseudoPath = "settings://$scopeName/${config.key}"
        mutationLog.register(pseudoPath, current)
        val write = shell.exec("settings put $scopeName \"${config.key}\" \"${config.value}\"")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return AutomationControllerResult.HardwareError("settings put rejected the write")
        }
        return AutomationControllerResult.Ok()
    }

    private suspend fun readCurrent(scope: String, key: String): String? {
        val result = shell.exec("settings get $scope \"$key\"")
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()
    }
}
