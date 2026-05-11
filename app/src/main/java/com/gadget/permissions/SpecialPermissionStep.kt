package com.gadget.permissions

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/**
 * One step in the special-permission onboarding sequence.
 *
 * @param id stable identifier used for analytics + restoring resume
 *           state across config changes.
 * @param needsRequest predicate the coordinator runs before launching
 *                     [buildIntent] — skips the step entirely when it
 *                     returns false.
 * @param buildIntent constructs the system Settings intent for this
 *                    step. Returning null means "not applicable on
 *                    this SDK level".
 */
data class SpecialPermissionStep(
    val id: String,
    val needsRequest: (Context) -> Boolean,
    val buildIntent: (Context) -> Intent?,
) {
    companion object {
        val allInDefaultOrder: List<SpecialPermissionStep> = listOf(
            SpecialPermissionStep(
                id = "dnd_policy_access",
                needsRequest = { ctx ->
                    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.isNotificationPolicyAccessGranted.not()
                },
                buildIntent = {
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
            SpecialPermissionStep(
                id = "battery_optimizations",
                needsRequest = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        false
                    } else {
                        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                        pm.isIgnoringBatteryOptimizations(ctx.packageName).not()
                    }
                },
                buildIntent = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        null
                    } else {
                        @Suppress("BatteryLife")
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                },
            ),
            SpecialPermissionStep(
                id = "draw_overlay",
                needsRequest = { ctx -> !Settings.canDrawOverlays(ctx) },
                buildIntent = { ctx ->
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
            SpecialPermissionStep(
                id = "device_admin",
                needsRequest = { ctx ->
                    val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val component = ComponentName(ctx, "com.gadget.receivers.AdminReceiver")
                    dpm.isAdminActive(component).not()
                },
                buildIntent = { ctx ->
                    val component = ComponentName(ctx, "com.gadget.receivers.AdminReceiver")
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
            SpecialPermissionStep(
                id = "write_settings",
                needsRequest = { ctx -> !Settings.System.canWrite(ctx) },
                buildIntent = { ctx ->
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
            SpecialPermissionStep(
                id = "schedule_exact_alarm",
                needsRequest = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        false
                    } else {
                        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        am.canScheduleExactAlarms().not()
                    }
                },
                buildIntent = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        null
                    } else {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                },
            ),
            SpecialPermissionStep(
                id = "usage_access",
                needsRequest = { ctx -> hasUsageAccess(ctx).not() },
                buildIntent = { ctx ->
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
        )

        /** True if the user has granted PACKAGE_USAGE_STATS via the AppOp. */
        fun hasUsageAccess(ctx: Context): Boolean {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName,
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        /** True if the user has granted the AppOp behind ACCESS_MOCK_LOCATION. */
        fun hasMockLocationAccess(ctx: Context): Boolean {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    ctx.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    ctx.packageName,
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
