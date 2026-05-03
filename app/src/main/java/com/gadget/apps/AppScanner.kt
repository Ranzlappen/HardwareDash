package com.gadget.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import com.gadget.data.db.apps.AppRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enumerates every launcher-visible Activity exposed by `LauncherApps` across all
 * `UserHandle`s available to the current user (which on supported devices includes
 * the work profile).
 *
 * Pure read-only scan: it never mutates persistence, never registers callbacks. The
 * caller (`AppRepository`) owns the storage side-effects.
 *
 * The `appKey` we mint here is stable across runs for a given (user, package,
 * activity) triple, so folder memberships keyed by `appKey` survive uninstall +
 * reinstall and even relaunches with different `LauncherActivityInfo` instances.
 */
@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scan(): List<AppRecord> {
        val launcherApps =
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: return emptyList()
        val userManager =
            context.getSystemService(Context.USER_SERVICE) as? UserManager
                ?: return emptyList()

        val now = System.currentTimeMillis()
        val out = mutableListOf<AppRecord>()

        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (t: SecurityException) {
                Timber.w(t, "AppScanner: getActivityList denied for user serial=%s", serial)
                continue
            }
            for (act in activities) {
                val pkg = act.applicationInfo.packageName
                val cls = act.componentName.className
                val key = "installed:$serial:$pkg/$cls"
                val rawLabel = act.label?.toString().orEmpty()
                out += AppRecord(
                    appKey = key,
                    packageName = pkg,
                    activityClass = cls,
                    label = if (rawLabel.isNotBlank()) rawLabel else pkg,
                    userSerial = serial,
                    isWebApk = pkg.startsWith(WEBAPK_PREFIX),
                    isWebLink = false,
                    firstInstallTime = act.firstInstallTime,
                    lastSeen = now,
                )
            }
        }
        return out
    }

    private companion object {
        // Chrome installs PWAs as real packages with this prefix.
        const val WEBAPK_PREFIX = "org.chromium.webapk."
    }
}
