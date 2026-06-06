package dev.ranzlappen.gadget.feature.apps.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the on-disk store for folder cover photos. Picked photo URIs from the
 * system photo picker aren't always persistable across reboots, so we copy
 * the bytes to `filesDir/folder_covers/<folderId>.png` and store that
 * absolute path in `Folder.coverIcon` as `image:<path>`.
 *
 * Photos are downsampled at decode time so a 4K gallery shot doesn't blow
 * past the LRU bitmap cap; the persisted PNG is ≤ 512 px square.
 */
@Singleton
class CoverImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File by lazy {
        File(context.filesDir, "folder_covers").apply { mkdirs() }
    }

    /**
     * Copies the picked [uri] into our store under [folderId]. Returns the
     * absolute file path on success or `null` if decoding / writing failed.
     * The caller is responsible for persisting `image:<returnedPath>` to
     * `Folder.coverIcon`.
     */
    suspend fun saveFromUri(folderId: Long, uri: Uri): String? = withContext(Dispatchers.IO) {
        val target = File(dir, "$folderId.png")
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            val downsampled = decodeDownsampled(bytes, MAX_EDGE_PX) ?: return@runCatching null
            FileOutputStream(target).use { out ->
                downsampled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            target.absolutePath
        }.getOrNull()
    }

    fun delete(folderId: Long) {
        File(dir, "$folderId.png").delete()
    }

    private fun decodeDownsampled(bytes: ByteArray, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = (maxOf(bounds.outWidth, bounds.outHeight) / maxEdge).coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private companion object {
        const val MAX_EDGE_PX = 512
    }
}
