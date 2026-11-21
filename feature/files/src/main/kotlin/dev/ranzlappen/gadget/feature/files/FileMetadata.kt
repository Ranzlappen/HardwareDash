package dev.ranzlappen.gadget.feature.files

import androidx.exifinterface.media.ExifInterface
import java.math.BigDecimal
import java.util.Locale

/** One label/value row. [label] is a technical name (EXIF tag) shown as is. */
data class MetaEntry(val label: String, val value: String)

/** Audio/video row whose label is a string resource. */
data class MediaEntry(val field: MediaField, val value: String)

enum class MediaField {
    Title, Artist, Album, AlbumArtist, Genre, Year, Track, Duration, Bitrate,
    SampleRate, Resolution, Rotation, FrameRate, Location, Tracks, Container,
}

/** Everything the Files screen shows about one picked file. */
data class FileMetadata(
    val name: String,
    val sizeBytes: Long?,
    val mimeType: String?,
    val modifiedMs: Long?,
    /** Pixel size of an image, `w × h`. */
    val dimensions: String? = null,
    /** Raw EXIF values by tag; null when the file is not an image or has no readable EXIF. */
    val exif: Map<String, String>? = null,
    /** Decimal latitude / longitude from EXIF GPS, if present. */
    val location: Pair<Double, Double>? = null,
    /** Audio/video metadata; null when the file is not audio/video. */
    val media: List<MediaEntry>? = null,
) {
    val extension: String? get() = name.substringAfterLast('.', "").lowercase().ifEmpty { null }
    val exifWritable: Boolean get() = exif != null && FileMetadataFormat.isExifWritable(mimeType)
}

/** What the last save / remove-location did. */
enum class FilesMessage { Saved, SaveFailed, LocationRemoved, ReadFailed }

/**
 * Pure tag tables and formatting for the Files screen (v3 B12), unit tested
 * in `FileMetadataFormatTest`.
 */
object FileMetadataFormat {

    /** Tags the user can edit, in display order. */
    val EDITABLE_TAGS: List<String> = listOf(
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
    )

    /** Read-only camera / file tags shown under the editable ones. */
    val READ_ONLY_TAGS: List<String> = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
    )

    /** Every GPS tag; cleared together by "Remove location". */
    val GPS_TAGS: List<String> = listOf(
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SPEED, ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_BEARING, ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_MAP_DATUM, ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS, ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP,
    )

    /** ExifInterface 1.3 can save JPEG, PNG and WebP. */
    fun isExifWritable(mimeType: String?): Boolean =
        mimeType?.lowercase() in setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    /**
     * Editable tags whose trimmed edit differs from the file; a blank edit
     * removes the tag (null).
     */
    fun exifChanges(original: Map<String, String>, edits: Map<String, String>): Map<String, String?> =
        EDITABLE_TAGS.mapNotNull { tag ->
            val before = original[tag]?.trim().orEmpty()
            val after = edits[tag]?.trim() ?: return@mapNotNull null
            if (after == before) null else tag to after.ifEmpty { null }
        }.toMap()

    /** Human-readable value for a read-only EXIF tag. */
    fun formatExif(tag: String, raw: String): String = when (tag) {
        ExifInterface.TAG_EXPOSURE_TIME -> raw.toDoubleOrNull()?.let { s ->
            if (s > 0 && s < 1) "1/${(1 / s).roundToIntString()} s" else "${trim(s)} s"
        } ?: raw
        ExifInterface.TAG_F_NUMBER -> raw.toDoubleOrNull()?.let { "f/${trim(it)}" } ?: raw
        ExifInterface.TAG_FOCAL_LENGTH -> rational(raw)?.let { "${trim(it)} mm" } ?: raw
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM -> "$raw mm"
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE -> rational(raw)?.let {
            (if (it > 0) "+" else "") + String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.') + " EV"
        } ?: raw
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO -> rational(raw)?.let { "${trim(it)}×" } ?: raw
        ExifInterface.TAG_GPS_ALTITUDE -> rational(raw)?.let { "${trim(it)} m" } ?: raw
        ExifInterface.TAG_ORIENTATION -> ORIENTATIONS[raw] ?: raw
        ExifInterface.TAG_FLASH -> raw.toIntOrNull()?.let { if (it and 1 == 1) "Fired" else "Did not fire" } ?: raw
        ExifInterface.TAG_WHITE_BALANCE -> when (raw) { "0" -> "Auto"; "1" -> "Manual"; else -> raw }
        else -> raw
    }

    fun formatLocation(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lon)

    /** `m:ss` / `h:mm:ss`. */
    fun formatDuration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun formatBitrate(bps: Long): String =
        if (bps >= 1_000_000) "${trim(bps / 1_000_000.0, 1)} Mbit/s" else "${bps / 1000} kbit/s"

    fun formatSampleRate(hz: Long): String = "${trim(hz / 1000.0, 1)} kHz"

    /** `"6900/1000"` → 6.9; plain numbers pass through. */
    fun rational(raw: String): Double? {
        val parts = raw.split('/')
        return when (parts.size) {
            1 -> parts[0].trim().toDoubleOrNull()
            2 -> {
                val n = parts[0].trim().toDoubleOrNull()
                val d = parts[1].trim().toDoubleOrNull()
                if (n == null || d == null || d == 0.0) null else n / d
            }
            else -> null
        }
    }

    private val ORIENTATIONS = mapOf(
        "1" to "Normal", "2" to "Mirrored", "3" to "Rotated 180°", "4" to "Mirrored, rotated 180°",
        "5" to "Mirrored, rotated 90°", "6" to "Rotated 90°", "7" to "Mirrored, rotated 270°", "8" to "Rotated 270°",
    )

    private fun Double.roundToIntString(): String = Math.round(this).toString()

    private fun trim(value: Double, decimals: Int = 2): String =
        BigDecimal(value).setScale(decimals, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
