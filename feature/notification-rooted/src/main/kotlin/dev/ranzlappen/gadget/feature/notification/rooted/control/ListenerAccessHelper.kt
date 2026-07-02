package dev.ranzlappen.gadget.feature.notification.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hard-coded in-app listener component name. The plan deliberately
 * does NOT add this service to the manifest — the helper still issues
 * the grant under root, but the OS will refuse to bind to a non-
 * existent service which surfaces cleanly as a no-op on real devices.
 * The mutation log entry is what matters: revert always works because
 * the helper records the synthesized pseudo-path on the way in.
 */
private const val LISTENER_COMPONENT =
    "com.gadget/com.gadget.notification.RootedNotificationListenerService"

/**
 * Grants and revokes notification-listener access via `cmd notification
 * allow_listener` / `disallow_listener`. The granted component is
 * hard-coded — callers cannot pass arbitrary component names.
 */
@Singleton
class ListenerAccessHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun allow(): NotificationControllerResult {
        val pseudoPath = "cmd-notification://listener/$LISTENER_COMPONENT"
        mutationLog.register(pseudoPath, "denied")
        val result = shell.exec("cmd notification allow_listener \"$LISTENER_COMPONENT\"")
        if (!result.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return NotificationControllerResult.HardwareError(
                "cmd notification allow_listener rejected the grant",
            )
        }
        return NotificationControllerResult.Ok(statusNote = "Listener granted: $LISTENER_COMPONENT")
    }
}
