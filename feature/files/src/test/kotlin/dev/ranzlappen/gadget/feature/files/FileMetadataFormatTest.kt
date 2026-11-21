package dev.ranzlappen.gadget.feature.files

import androidx.exifinterface.media.ExifInterface
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class FileMetadataFormatTest {

    @Test
    fun `exifChanges reports only edited editable tags, blank meaning remove`() {
        val original = mapOf(
            ExifInterface.TAG_ARTIST to "Alice",
            ExifInterface.TAG_MAKE to "Xiaomi",
            ExifInterface.TAG_F_NUMBER to "1.7",
        )
        val edits = mapOf(
            ExifInterface.TAG_ARTIST to "Bob ",
            ExifInterface.TAG_MAKE to "",
            ExifInterface.TAG_COPYRIGHT to "",
            ExifInterface.TAG_MODEL to "  ",
            ExifInterface.TAG_F_NUMBER to "2.0", // read-only: ignored
        )
        assertEquals(
            mapOf(ExifInterface.TAG_ARTIST to "Bob", ExifInterface.TAG_MAKE to null),
            FileMetadataFormat.exifChanges(original, edits),
        )
        assertTrue(FileMetadataFormat.exifChanges(original, mapOf(ExifInterface.TAG_ARTIST to "Alice")).isEmpty())
    }

    @Test
    fun `read-only EXIF values are formatted for people`() {
        assertEquals("1/125 s", FileMetadataFormat.formatExif(ExifInterface.TAG_EXPOSURE_TIME, "0.008"))
        assertEquals("2 s", FileMetadataFormat.formatExif(ExifInterface.TAG_EXPOSURE_TIME, "2"))
        assertEquals("f/1.8", FileMetadataFormat.formatExif(ExifInterface.TAG_F_NUMBER, "1.8"))
        assertEquals("6.9 mm", FileMetadataFormat.formatExif(ExifInterface.TAG_FOCAL_LENGTH, "6900/1000"))
        assertEquals("-0.33 EV", FileMetadataFormat.formatExif(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, "-1/3"))
        assertEquals("+1 EV", FileMetadataFormat.formatExif(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, "3/3"))
        assertEquals("Rotated 90°", FileMetadataFormat.formatExif(ExifInterface.TAG_ORIENTATION, "6"))
        assertEquals("Fired", FileMetadataFormat.formatExif(ExifInterface.TAG_FLASH, "25"))
        assertEquals("Did not fire", FileMetadataFormat.formatExif(ExifInterface.TAG_FLASH, "16"))
        assertEquals("weird", FileMetadataFormat.formatExif(ExifInterface.TAG_F_NUMBER, "weird"))
    }

    @Test
    fun `rational parsing`() {
        assertEquals(6.9, FileMetadataFormat.rational("6900/1000")!!, 1e-9)
        assertEquals(4.0, FileMetadataFormat.rational("4")!!, 1e-9)
        assertNull(FileMetadataFormat.rational("1/0"))
        assertNull(FileMetadataFormat.rational("a/b"))
    }

    @Test
    fun `media and location formatting`() {
        assertEquals("3:25", FileMetadataFormat.formatDuration(205_000))
        assertEquals("1:00:00", FileMetadataFormat.formatDuration(3_600_000))
        assertEquals("320 kbit/s", FileMetadataFormat.formatBitrate(320_000))
        assertEquals("12.5 Mbit/s", FileMetadataFormat.formatBitrate(12_500_000))
        assertEquals("44.1 kHz", FileMetadataFormat.formatSampleRate(44_100))
        assertEquals("52.52000, 13.40500", FileMetadataFormat.formatLocation(52.52, 13.405))
    }

    @Test
    fun `writable formats and extension`() {
        assertTrue(FileMetadataFormat.isExifWritable("image/jpeg"))
        assertTrue(FileMetadataFormat.isExifWritable("image/webp"))
        assertFalse(FileMetadataFormat.isExifWritable("image/heic"))
        assertFalse(FileMetadataFormat.isExifWritable(null))
        val meta = FileMetadata("IMG.JPG", 1, "image/jpeg", null, exif = emptyMap())
        assertEquals("jpg", meta.extension)
        assertTrue(meta.exifWritable)
        assertNull(FileMetadata("README", 1, "text/plain", null).extension)
    }

    @Test
    fun `remove location clears every GPS tag`() {
        assertTrue(ExifInterface.TAG_GPS_LATITUDE in FileMetadataFormat.GPS_TAGS)
        assertTrue(ExifInterface.TAG_GPS_LONGITUDE_REF in FileMetadataFormat.GPS_TAGS)
        assertTrue(FileMetadataFormat.GPS_TAGS.all { it.startsWith("GPS") })
    }
}
