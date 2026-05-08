package com.gadget.permissions

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
                    !nm.isNotificationPolicyAccessGranted
                },
                buildIntent = {
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
            SpecialPermissionStep(
                id = "battery_optimizations",
                needsRequest = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@SpecialPermissionStep false
                    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                    !pm.isIgnoringBatteryOptimizations(ctx.packageName)
                },
                buildIntent = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@SpecialPermissionStep null
                    @Suppress("BatteryLife")
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                    !dpm.isAdminActive(component)
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
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@SpecialPermissionStep false
                    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    !am.canScheduleExactAlarms()
                },
                buildIntent = { ctx ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@SpecialPermissionStep null
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            ),
        )
    }
}
