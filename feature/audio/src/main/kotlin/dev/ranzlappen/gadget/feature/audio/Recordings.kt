package dev.ranzlappen.gadget.feature.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A WAV file the recorder saved to `Music/Gadget`. */
data class Recording(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAtMs: Long,
)

/**
 * The recordings list (v3 B10): what [AudioRecorder] wrote to MediaStore under
 * [RELATIVE_PATH]. The app owns those rows, so listing and deleting need no
 * extra storage permission on Android 10+.
 */
class Recordings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun list(): List<Recording> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val result = mutableListOf<Recording>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                arrayOf("$RELATIVE_PATH%"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val name = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val size = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val added = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val bytes = c.getLong(size)
                    result += Recording(
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(id)),
                        name = c.getString(name) ?: "",
                        // MediaStore fills DURATION only after its scan; our
                        // files are plain 44.1 kHz mono 16-bit WAV, so derive it.
                        durationMs = c.getLong(duration).takeIf { it > 0 } ?: wavDurationMs(bytes),
                        sizeBytes = bytes,
                        addedAtMs = c.getLong(added) * 1000,
                    )
                }
            }
        } catch (_: SecurityException) {
            // Nothing listable — show an empty list rather than crash.
        }
        result
    }

    /** True when the row is gone. Fails for files another app owns. */
    suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val RELATIVE_PATH = "Music/Gadget"
    }
}

/** Duration of a 44-byte-header, 44.1 kHz mono 16-bit WAV of [sizeBytes]. */
internal fun wavDurationMs(sizeBytes: Long): Long =
    ((sizeBytes - WAV_HEADER_BYTES).coerceAtLeast(0) * 1000) / (AUDIO_SAMPLE_RATE * 2)

/** `Gadget_20261121_031500.wav` — sorts by time, readable in a file manager. */
internal fun recordingFileName(epochMs: Long): String =
    "Gadget_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(epochMs)) + ".wav"

/** `m:ss` (or `h:mm:ss`). */
internal fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

internal const val AUDIO_SAMPLE_RATE = 44_100
private const val WAV_HEADER_BYTES = 44L
