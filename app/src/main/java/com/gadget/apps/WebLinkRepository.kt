package com.gadget.apps

import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.WebLinkApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owner of the `apps_weblink` table and the synthetic AppRecord rows that mirror
 * each web-link app, so folder code can treat web-links and installed apps
 * uniformly via `AppRecord`.
 *
 * Synthetic-record convention:
 *  - `appKey`        = `weblink:<id>`
 *  - `packageName`   = `weblink` (sentinel; never collides with a real package)
 *  - `isWebLink`     = true  (drives launcher dispatch in batch 7)
 */
@Singleton
class WebLinkRepository @Inject constructor(
    private val dao: AppsDao,
    private val faviconFetcher: FaviconFetcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val webLinks: Flow<List<WebLinkApp>> = dao.observeWebLinks()

    suspend fun add(url: String, label: String): Long {
        val now = System.currentTimeMillis()
        val effectiveLabel = label.ifBlank { url }
        val id = dao.insertWebLink(
            WebLinkApp(
                url = url,
                label = effectiveLabel,
                faviconPath = null,
                createdAt = now,
            ),
        )
        dao.upsertAppRecord(toAppRecord(id, effectiveLabel, createdAt = now, lastSeen = now))
        scope.launch { fetchFaviconAndPersist(id, url) }
        return id
    }

    suspend fun update(link: WebLinkApp) {
        dao.updateWebLink(link)
        dao.upsertAppRecord(
            toAppRecord(
                id = link.id,
                label = link.label,
                createdAt = link.createdAt,
                lastSeen = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun remove(id: Long) {
        val link = dao.getWebLink(id) ?: return
        dao.deleteWebLink(id)
        dao.deleteAppRecord("weblink:$id")
        scope.launch { faviconFetcher.delete(link.url) }
    }

    suspend fun getById(id: Long): WebLinkApp? = dao.getWebLink(id)

    private suspend fun fetchFaviconAndPersist(id: Long, url: String) {
        val path = faviconFetcher.fetch(url) ?: return
        val link = dao.getWebLink(id) ?: return
        dao.updateWebLink(link.copy(faviconPath = path))
    }

    private fun toAppRecord(
        id: Long,
        label: String,
        createdAt: Long,
        lastSeen: Long,
    ): AppRecord = AppRecord(
        appKey = "weblink:$id",
        packageName = "weblink",
        activityClass = null,
        label = label,
        userSerial = 0L,
        isWebApk = false,
        isWebLink = true,
        firstInstallTime = createdAt,
        lastSeen = lastSeen,
    )
}
