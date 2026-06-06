package dev.ranzlappen.gadget.feature.apps

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort fetcher for the favicon of a user-added web-link "app". Tries
 * `<scheme>://<host>/favicon.ico` and `…/favicon.png` directly — never reaches a
 * third-party indexing service, so the user's URL list stays private.
 *
 * Cache layout: `filesDir/apps_favicons/<sha1(url)>` (no extension; format is
 * decoded by the consumer). Returning null means "no favicon — render a
 * generated monogram chip instead", which the UI layer handles.
 */
@Singleton
class FaviconFetcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "apps_favicons").apply { mkdirs() }
    }

    suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        val parsed = runCatching { URL(url) }.getOrNull() ?: return@withContext null
        val host = parsed.host?.takeIf { it.isNotBlank() } ?: return@withContext null
        val target = File(cacheDir, sha1(url))
        if (target.exists() && target.length() > 0) return@withContext target.absolutePath

        val candidates = listOf(
            "${parsed.protocol}://$host/favicon.ico",
            "${parsed.protocol}://$host/favicon.png",
        )
        for (candidate in candidates) {
            if (download(candidate, target)) return@withContext target.absolutePath
        }
        null
    }

    suspend fun delete(url: String) = withContext(Dispatchers.IO) {
        File(cacheDir, sha1(url)).delete()
        Unit
    }

    private fun download(url: String, dest: File): Boolean = try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.length() > 0
            } else {
                false
            }
        } finally {
            conn.disconnect()
        }
    } catch (t: Throwable) {
        Timber.d(t, "FaviconFetcher: download failed for %s", url)
        false
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
    }
}
