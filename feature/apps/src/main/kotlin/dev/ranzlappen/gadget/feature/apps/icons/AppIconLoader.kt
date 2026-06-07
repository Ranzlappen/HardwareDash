package dev.ranzlappen.gadget.feature.apps.icons

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.feature.apps.WebLinkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves an [AppRecord] to a real launcher / WebAPK icon (or a deterministic
 * monogram fallback). Used by every UI surface that displays an app — the
 * folder editor's picker, the popup grid, the widget renderer's preview tiles.
 *
 * Resolution chain:
 *   1. Web-link records → decoded `faviconPath` if present, else monogram
 *      seeded by the URL.
 *   2. Installed records → `LauncherApps.getActivityList(pkg, user)` (handles
 *      work-profile + WebAPK uniformly), falling through to
 *      `PackageManager.getApplicationIcon` and ultimately a monogram.
 *
 * Bitmaps are cached in an `LruCache(128)` keyed by `appKey`. Each cached
 * bitmap is at most ~36 KB at the default 96-px size, so the cache caps at
 * roughly 4.5 MB.
 */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webLinkRepository: WebLinkRepository,
    private val monogramChipFactory: MonogramChipFactory,
) {
    private val cache: LruCache<String, Bitmap> = LruCache(CACHE_SIZE)
    private val mutex = Mutex()

    suspend fun loadBitmap(record: AppRecord, sizePx: Int = DEFAULT_SIZE_PX): Bitmap =
        withContext(Dispatchers.IO) {
            cache.get(record.appKey)?.let { return@withContext it }
            mutex.withLock {
                cache.get(record.appKey)?.let { return@withContext it }
                val bmp = resolve(record, sizePx)
                cache.put(record.appKey, bmp)
                bmp
            }
        }

    suspend fun loadImageBitmap(record: AppRecord, sizePx: Int = DEFAULT_SIZE_PX): ImageBitmap =
        loadBitmap(record, sizePx).asImageBitmap()

    private suspend fun resolve(record: AppRecord, sizePx: Int): Bitmap {
        if (record.isWebLink) return resolveWebLink(record, sizePx)

        val launcherApps =
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        val user: UserHandle? = userManager?.userProfiles?.firstOrNull {
            userManager.getSerialNumberForUser(it) == record.userSerial
        }

        val drawable = runCatching {
            if (launcherApps != null && user != null) {
                launcherApps.getActivityList(record.packageName, user)
                    .firstOrNull()
                    ?.getBadgedIcon(0)
            } else {
                null
            }
        }.getOrNull() ?: runCatching {
            // PackageManager fallback covers edge cases where LauncherApps
            // returned null (e.g. transient state right after install).
            context.packageManager.getApplicationIcon(record.packageName)
        }.getOrNull()

        return drawable?.let {
            runCatching { it.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888) }.getOrNull()
        } ?: monogramChipFactory.build(record.label, record.appKey, sizePx)
    }

    private suspend fun resolveWebLink(record: AppRecord, sizePx: Int): Bitmap {
        val id = record.appKey.removePrefix("weblink:").toLongOrNull()
        if (id != null) {
            val link = runCatching { webLinkRepository.getById(id) }.getOrNull()
            val path = link?.faviconPath
            if (path != null) {
                val decoded = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                if (decoded != null) return decoded.scaleTo(sizePx)
            }
        }
        return monogramChipFactory.build(record.label, record.appKey, sizePx)
    }

    private fun Bitmap.scaleTo(size: Int): Bitmap {
        if (width == size && height == size) return this
        return Bitmap.createScaledBitmap(this, size, size, true)
    }

    companion object {
        const val DEFAULT_SIZE_PX = 96
        private const val CACHE_SIZE = 128
    }
}
