package dev.ranzlappen.gadget.feature.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the materialized "what apps exist on this device"
 * cache backing the App-Organizer module. Keeps `apps_record` in sync with
 * reality via:
 *  - an initial scan on first instantiation (Hilt singleton init)
 *  - explicit `requestRefresh()` from the app entry point
 *  - a `LauncherApps.Callback` that fires on PACKAGE_ADDED / REMOVED / CHANGED
 *    and on PACKAGES_AVAILABLE / UNAVAILABLE (work-profile state changes)
 *
 * Scan requests are conflated through a `Channel.CONFLATED`, so a burst of
 * package events triggers at most ~2 sequential scans. Web-link records are
 * NOT touched by this repository — they're owned by the web-link table.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: AppScanner,
    private val dao: AppsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshLock = Mutex()
    private val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

    val appRecords: Flow<List<AppRecord>> = dao.observeAppRecords()

    init {
        scope.launch {
            for (event in refreshTrigger) {
                try {
                    refresh()
                } catch (t: Throwable) {
                    Timber.e(t, "AppRepository refresh failed")
                }
            }
        }
        registerLauncherCallback()
        // Always do at least one scan after construction so widgets that wake the
        // process before the app UI still see a populated cache.
        refreshTrigger.trySend(Unit)
    }

    /** Public entry point used by the app's startup path to force a fresh scan. */
    fun requestRefresh() {
        refreshTrigger.trySend(Unit)
    }

    suspend fun refresh() = refreshLock.withLock {
        val current = scanner.scan()
        val currentKeys = HashSet<String>(current.size).also { set ->
            current.forEach { set.add(it.appKey) }
        }
        val existing = dao.getAppRecords()
        // Only prune installed-app entries; web-link rows belong to a different
        // owner and must not be deleted by package-scan diffs.
        val toDelete = existing.filter { !it.isWebLink && it.appKey !in currentKeys }
        for (r in toDelete) dao.deleteAppRecord(r.appKey)
        if (current.isNotEmpty()) dao.upsertAppRecords(current)
        Timber.d(
            "AppRepository.refresh: scanned=%d, dropped=%d",
            current.size,
            toDelete.size,
        )
    }

    private fun registerLauncherCallback() {
        val launcherApps =
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = trigger()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = trigger()
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = trigger()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = trigger()
            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = trigger()

            private fun trigger() {
                refreshTrigger.trySend(Unit)
            }
        })
    }
}
