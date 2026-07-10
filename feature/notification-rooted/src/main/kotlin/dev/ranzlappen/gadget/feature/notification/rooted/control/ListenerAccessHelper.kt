package dev.ranzlappen.gadget.feature.notification.rooted.control

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fully-qualified class name of the module's real
 * `GadgetNotificationListenerService` (declared + manifest-registered in the
 * base `:feature:notification` module with `BIND_NOTIFICATION_LISTENER_SERVICE`).
 *
 * Earlier revision of this helper hard-coded a component
 * (`com.gadget/com.gadget.notification.RootedNotificationListenerService`)
 * that was never declared in any manifest — the grant command ran but had no
 * real listener to attach to. That's fixed: [allow] now targets the actual
 * service class, and pairs it with [Context.getPackageName] at call time (the
 * OS component-name format is `<packageName>/<fully-qualified class name>`,
 * and the class name is stable across app IDs while the package name is not).
 */
private const val LISTENER_SERVICE_CLASS =
    "dev.ranzlappen.gadget.feature.notification.listener.GadgetNotificationListenerService"

/**
 * Grants and revokes notification-listener access via `cmd notification
 * allow_listener` / `disallow_listener`. The granted component is
 * hard-coded to this module's own listener service — callers cannot pass
 * arbitrary component names.
 */
@Singleton
class ListenerAccessHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun allow(): NotificationControllerResult {
        val component = "${context.packageName}/$LISTENER_SERVICE_CLASS"
        val pseudoPath = "cmd-notification://listener/$component"
        mutationLog.register(pseudoPath, "denied")
        val result = shell.exec("cmd notification allow_listener \"$component\"")
        if (!result.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return NotificationControllerResult.HardwareError(
                "cmd notification allow_listener rejected the grant",
            )
        }
        return NotificationControllerResult.Ok(statusNote = "Listener granted: $component")
    }
}
