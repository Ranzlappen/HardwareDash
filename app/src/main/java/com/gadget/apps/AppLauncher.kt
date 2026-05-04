package com.gadget.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import com.gadget.data.db.apps.AppRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for launching whatever an [AppRecord] points at:
 *  - installed activities (incl. work-profile) via `LauncherApps.startMainActivity`
 *  - web-link "apps" via [WebLinkLauncher]
 *
 * Returns true on success so callers (folder popup, widget) can stay open or
 * dismiss based on whether the launch went through.
 */
@Singleton
class AppLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webLinkLauncher: WebLinkLauncher,
    private val webLinkRepository: WebLinkRepository,
) {
    suspend fun launch(record: AppRecord): Boolean {
        if (record.isWebLink) {
            val id = record.appKey.removePrefix("weblink:").toLongOrNull() ?: return false
            val link = webLinkRepository.getById(id) ?: return false
            return webLinkLauncher.launch(link.url)
        }

        val launcherApps =
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: return false
        val userManager =
            context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return false

        val user: UserHandle = userManager.userProfiles.firstOrNull {
            userManager.getSerialNumberForUser(it) == record.userSerial
        } ?: return false

        val activityClass = record.activityClass ?: return false
        val component = ComponentName(record.packageName, activityClass)

        return try {
            launcherApps.startMainActivity(component, user, null, null)
            true
        } catch (t: Throwable) {
            Timber.w(t, "AppLauncher: failed to start %s", record.appKey)
            false
        }
    }
}
