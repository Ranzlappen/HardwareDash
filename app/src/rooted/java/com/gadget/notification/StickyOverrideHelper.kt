package com.gadget.notification

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val IMPORTANCE_LOW = 2
internal const val IMPORTANCE_HIGH = 4

/**
 * Channel-importance override via `cmd notification`. Always raises
 * `IMPORTANCE_LOW` to `IMPORTANCE_HIGH`; never silences. Snapshot+
 * restore via the shared mutation log under
 * `cmd-notification://channel/<id>/importance`.
 */
@Singleton
class StickyOverrideHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun raiseImportance(
        packageName: String,
        channelId: String,
    ): NotificationControllerResult {
        val current = readImportance(packageName, channelId)
            ?: return NotificationControllerResult.HardwareError(
                "could not read importance for $channelId",
            )
        val pseudoPath = "cmd-notification://channel/$channelId/importance"
        mutationLog.register(pseudoPath, current.toString())
        val write = shell.exec(
            "cmd notification set_channel_importance \"$packageName\" \"$channelId\" $IMPORTANCE_HIGH",
        )
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return NotificationControllerResult.HardwareError(
                "cmd notification rejected the importance write",
            )
        }
        return NotificationControllerResult.ChannelImportanceSnapshot(
            channelId = channelId,
            previousImportance = current,
            newImportance = IMPORTANCE_HIGH,
        )
    }

    private suspend fun readImportance(packageName: String, channelId: String): Int? {
        val result = shell.exec(
            "cmd notification get_channel_importance \"$packageName\" \"$channelId\"",
        )
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()?.toIntOrNull()
    }
}
