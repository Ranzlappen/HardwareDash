package dev.ranzlappen.gadget.feature.youtubedownloader.storage

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dev.ranzlappen.gadget.feature.youtubedownloader.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Publishes finished downloads from a private working directory into the
 * shared MediaStore collections (`Movies/HardwareDash` for video,
 * `Music/HardwareDash` for audio), so they show up in Gallery / Files / music
 * apps. Scoped-storage friendly — no `WRITE_EXTERNAL_STORAGE` on minSdk 29+.
 *
 * Playlist sub-folders produced by yt-dlp's output template are preserved as
 * MediaStore `RELATIVE_PATH` sub-directories.
 */
object MediaStoreExporter {

    /**
     * Copy every file under [sourceDir] into the appropriate MediaStore
     * collection, deleting each source file as it is published. Returns the
     * number of files exported.
     */
    suspend fun publish(context: Context, sourceDir: File, kind: MediaKind): Int =
        withContext(Dispatchers.IO) {
            if (!sourceDir.isDirectory) return@withContext 0
            val resolver = context.contentResolver
            val collection = when (kind) {
                MediaKind.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val baseDir = if (kind == MediaKind.AUDIO) {
                Environment.DIRECTORY_MUSIC
            } else {
                Environment.DIRECTORY_MOVIES
            }

            var exported = 0
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                // Preserve any playlist sub-folder yt-dlp created.
                val subDir = file.parentFile?.relativeToOrNull(sourceDir)?.path?.takeIf { it.isNotEmpty() }
                val relativePath = listOfNotNull(baseDir, ROOT_FOLDER, subDir).joinToString("/") + "/"

                val pending = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeOf(file))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = runCatching { resolver.insert(collection, pending) }.getOrNull()
                if (uri == null) {
                    Timber.w("MediaStore insert returned null for %s", file.name)
                    return@forEach
                }
                runCatching {
                    resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }.onSuccess {
                    file.delete()
                    exported++
                }.onFailure { e ->
                    Timber.w(e, "MediaStore export failed for %s", file.name)
                    runCatching { resolver.delete(uri, null, null) }
                }
            }
            exported
        }

    private fun mimeTypeOf(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "m4a", "mp4" -> if (ext == "m4a") "audio/mp4" else "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "opus", "ogg" -> "audio/ogg"
                "mp3" -> "audio/mpeg"
                else -> "application/octet-stream"
            }
    }

    private const val ROOT_FOLDER = "HardwareDash"
}
