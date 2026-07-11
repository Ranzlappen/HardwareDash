package dev.ranzlappen.gadget.core.permissions

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * The single source of truth mapping each [SpecialPermission] to its live
 * grant-state query and its Settings deep-link — unifying the overlay /
 * exact-alarm / WRITE_SETTINGS / notification-listener / all-files handling
 * that was scattered across `AccessibilityCard`, `automation-ui`,
 * `display`, and `notification`.
 */
object SpecialPermissions {

    /** Whether [special] is currently granted on this device. */
    fun isGranted(context: Context, special: SpecialPermission): Boolean = when (special) {
        SpecialPermission.Overlay -> Settings.canDrawOverlays(context)
        SpecialPermission.ExactAlarm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
        SpecialPermission.WriteSettings -> Settings.System.canWrite(context)
        SpecialPermission.NotificationListener -> {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            enabled.contains(context.packageName)
        }
        SpecialPermission.AllFilesAccess -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * The Settings intent to grant [special]. Callers wrap `startActivity`
     * in `runCatching` — some OEM builds lack a handler for a given action.
     */
    fun settingsIntent(context: Context, special: SpecialPermission): Intent {
        val pkgUri = Uri.fromParts("package", context.packageName, null)
        return when (special) {
            SpecialPermission.Overlay ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)
            SpecialPermission.ExactAlarm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri)
                } else {
                    appDetailsIntent(pkgUri)
                }
            SpecialPermission.WriteSettings ->
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri)
            SpecialPermission.NotificationListener ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            SpecialPermission.AllFilesAccess ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkgUri)
                } else {
                    appDetailsIntent(pkgUri)
                }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** The app-details Settings page — the universal fallback affordance. */
    fun appDetailsIntent(context: Context): Intent =
        appDetailsIntent(Uri.fromParts("package", context.packageName, null))

    private fun appDetailsIntent(pkgUri: Uri): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
