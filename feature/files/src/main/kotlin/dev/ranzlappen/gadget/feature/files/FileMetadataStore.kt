package dev.ranzlappen.gadget.feature.files

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a picked file's metadata and writes image EXIF back (v3 B12).
 *
 * Writes go through a temp copy: the file is copied to cache, ExifInterface
 * rewrites the copy, and only then is the original opened truncating
 * (`"wt"`) and replaced. If the provider refuses write access the original
 * is never touched.
 */
class FileMetadataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun read(uri: Uri): FileMetadata? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = uri.lastPathSegment ?: "?"
        var size: Long? = null
        var modified: Long? = null
        try {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                        ?.let { i -> c.getString(i)?.let { name = it } }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }
                        ?.let { size = c.getLong(it) }
                    c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED).takeIf { it >= 0 && !c.isNull(it) }
                        ?.let { i -> c.getLong(i).takeIf { it > 0 }?.let { modified = it } }
                }
            }
        } catch (_: SecurityException) {
            return@withContext null
        } catch (_: IllegalArgumentException) {
            // Some providers reject a null projection; name falls back to the URI.
        }
        val mime = resolver.getType(uri)
        var metadata = FileMetadata(name = name, sizeBytes = size, mimeType = mime, modifiedMs = modified)
        if (mime?.startsWith("image/") == true) metadata = readImage(uri, metadata)
        if (mime?.startsWith("audio/") == true || mime?.startsWith("video/") == true) {
            metadata = metadata.copy(media = readMedia(uri))
        }
        metadata
    }

    private fun readImage(uri: Uri, base: FileMetadata): FileMetadata {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        val dimensions = if (bounds.outWidth > 0 && bounds.outHeight > 0) "${bounds.outWidth} × ${bounds.outHeight}" else null
        val exif = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        val tags = exif?.let { e ->
            (FileMetadataFormat.EDITABLE_TAGS + FileMetadataFormat.READ_ONLY_TAGS)
                .mapNotNull { tag -> e.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let { tag to it } }
                .toMap()
        }
        val location = exif?.latLong?.let { it[0] to it[1] }
        return base.copy(dimensions = dimensions, exif = tags, location = location)
    }

    private fun readMedia(uri: Uri): List<MediaEntry> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            fun get(key: Int) = retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }
            buildList {
                get(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { add(MediaEntry(MediaField.Title, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { add(MediaEntry(MediaField.Artist, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { add(MediaEntry(MediaField.Album, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.let { add(MediaEntry(MediaField.AlbumArtist, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { add(MediaEntry(MediaField.Genre, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_YEAR)?.let { add(MediaEntry(MediaField.Year, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.let { add(MediaEntry(MediaField.Track, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?.let { add(MediaEntry(MediaField.Duration, FileMetadataFormat.formatDuration(it))) }
                get(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
                    ?.let { add(MediaEntry(MediaField.Bitrate, FileMetadataFormat.formatBitrate(it))) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    get(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toLongOrNull()
                        ?.let { add(MediaEntry(MediaField.SampleRate, FileMetadataFormat.formatSampleRate(it))) }
                }
                val w = get(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = get(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (w != null && h != null) add(MediaEntry(MediaField.Resolution, "$w × $h"))
                get(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.takeIf { it != "0" }
                    ?.let { add(MediaEntry(MediaField.Rotation, "$it°")) }
                get(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.let { add(MediaEntry(MediaField.FrameRate, "$it fps")) }
                get(MediaMetadataRetriever.METADATA_KEY_LOCATION)?.let { add(MediaEntry(MediaField.Location, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.let { add(MediaEntry(MediaField.Tracks, it)) }
                get(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let { add(MediaEntry(MediaField.Container, it)) }
            }
        } catch (_: RuntimeException) {
            emptyList() // unsupported / DRM / unreadable stream
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Applies [changes] (null = remove the tag). True when the file was rewritten. */
    suspend fun writeExif(uri: Uri, changes: Map<String, String?>): Boolean = withContext(Dispatchers.IO) {
        if (changes.isEmpty()) return@withContext true
        val resolver = context.contentResolver
        val temp = File.createTempFile("exif", ".tmp", context.cacheDir)
        try {
            resolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } }
                ?: return@withContext false
            val exif = ExifInterface(temp.absolutePath)
            changes.forEach { (tag, value) -> exif.setAttribute(tag, value) }
            exif.saveAttributes()
            resolver.openOutputStream(uri, "wt")?.use { out -> temp.inputStream().use { it.copyTo(out) } }
                ?: return@withContext false
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: UnsupportedOperationException) {
            false // provider doesn't support "wt"
        } catch (_: IllegalArgumentException) {
            false
        } finally {
            temp.delete()
        }
    }

    suspend fun removeLocation(uri: Uri): Boolean =
        writeExif(uri, FileMetadataFormat.GPS_TAGS.associateWith { null })
}
