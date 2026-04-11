package com.gadget.ui.screens

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// All EXIF tags that can be edited (expanded list)
private val EDITABLE_EXIF_TAGS = listOf(
    ExifInterface.TAG_ARTIST to "Artist",
    ExifInterface.TAG_COPYRIGHT to "Copyright",
    ExifInterface.TAG_IMAGE_DESCRIPTION to "Description",
    ExifInterface.TAG_SOFTWARE to "Software",
    ExifInterface.TAG_MAKE to "Camera Make",
    ExifInterface.TAG_MODEL to "Camera Model",
    ExifInterface.TAG_USER_COMMENT to "User Comment",
    ExifInterface.TAG_DATETIME to "Date/Time",
    ExifInterface.TAG_DATETIME_ORIGINAL to "Date/Time Original",
    ExifInterface.TAG_DATETIME_DIGITIZED to "Date/Time Digitized",
    ExifInterface.TAG_GPS_LATITUDE to "GPS Latitude",
    ExifInterface.TAG_GPS_LONGITUDE to "GPS Longitude",
    ExifInterface.TAG_GPS_ALTITUDE to "GPS Altitude",
    ExifInterface.TAG_SHUTTER_SPEED_VALUE to "Shutter Speed",
    ExifInterface.TAG_APERTURE_VALUE to "Aperture",
    ExifInterface.TAG_METERING_MODE to "Metering Mode",
    ExifInterface.TAG_LIGHT_SOURCE to "Light Source",
    ExifInterface.TAG_SCENE_TYPE to "Scene Type",
    ExifInterface.TAG_LENS_MAKE to "Lens Make",
    ExifInterface.TAG_LENS_MODEL to "Lens Model",
)

// All EXIF tags to read (display-only + editable)
private val ALL_EXIF_TAGS = listOf(
    ExifInterface.TAG_IMAGE_WIDTH to "Width",
    ExifInterface.TAG_IMAGE_LENGTH to "Height",
    ExifInterface.TAG_ORIENTATION to "Orientation",
    ExifInterface.TAG_DATETIME to "Date/Time",
    ExifInterface.TAG_DATETIME_ORIGINAL to "Date/Time Original",
    ExifInterface.TAG_DATETIME_DIGITIZED to "Date/Time Digitized",
    ExifInterface.TAG_MAKE to "Camera Make",
    ExifInterface.TAG_MODEL to "Camera Model",
    ExifInterface.TAG_F_NUMBER to "F-Number",
    ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
    ExifInterface.TAG_SHUTTER_SPEED_VALUE to "Shutter Speed",
    ExifInterface.TAG_APERTURE_VALUE to "Aperture",
    ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO",
    ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
    ExifInterface.TAG_FLASH to "Flash",
    ExifInterface.TAG_WHITE_BALANCE to "White Balance",
    ExifInterface.TAG_METERING_MODE to "Metering Mode",
    ExifInterface.TAG_LIGHT_SOURCE to "Light Source",
    ExifInterface.TAG_SCENE_TYPE to "Scene Type",
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO to "Digital Zoom Ratio",
    ExifInterface.TAG_SCENE_CAPTURE_TYPE to "Scene Capture Type",
    ExifInterface.TAG_CONTRAST to "Contrast",
    ExifInterface.TAG_SATURATION to "Saturation",
    ExifInterface.TAG_SHARPNESS to "Sharpness",
    ExifInterface.TAG_IMAGE_UNIQUE_ID to "Image Unique ID",
    ExifInterface.TAG_CAMERA_OWNER_NAME to "Camera Owner",
    ExifInterface.TAG_BODY_SERIAL_NUMBER to "Body Serial Number",
    ExifInterface.TAG_LENS_MAKE to "Lens Make",
    ExifInterface.TAG_LENS_MODEL to "Lens Model",
    ExifInterface.TAG_GPS_LATITUDE to "GPS Latitude",
    ExifInterface.TAG_GPS_LONGITUDE to "GPS Longitude",
    ExifInterface.TAG_GPS_ALTITUDE to "GPS Altitude",
    ExifInterface.TAG_ARTIST to "Artist",
    ExifInterface.TAG_COPYRIGHT to "Copyright",
    ExifInterface.TAG_IMAGE_DESCRIPTION to "Description",
    ExifInterface.TAG_SOFTWARE to "Software",
    ExifInterface.TAG_USER_COMMENT to "User Comment",
)

// Set of editable tag constants for quick lookup
private val EDITABLE_TAG_KEYS = EDITABLE_EXIF_TAGS.map { it.first }.toSet()

// Editable media metadata tags for audio/video (MediaMetadataRetriever key -> label)
private val EDITABLE_MEDIA_TAGS = listOf(
    MediaMetadataRetriever.METADATA_KEY_TITLE to "Title",
    MediaMetadataRetriever.METADATA_KEY_ARTIST to "Artist",
    MediaMetadataRetriever.METADATA_KEY_ALBUM to "Album",
    MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST to "Album Artist",
    MediaMetadataRetriever.METADATA_KEY_COMPOSER to "Composer",
    MediaMetadataRetriever.METADATA_KEY_WRITER to "Writer",
    MediaMetadataRetriever.METADATA_KEY_GENRE to "Genre",
    MediaMetadataRetriever.METADATA_KEY_YEAR to "Year",
    MediaMetadataRetriever.METADATA_KEY_DATE to "Date",
    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "Track Number",
    MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER to "Disc Number",
)

private val EDITABLE_MEDIA_KEYS = EDITABLE_MEDIA_TAGS.map { it.first }.toSet()

// All media metadata tags to read
private val ALL_MEDIA_TAGS = listOf(
    MediaMetadataRetriever.METADATA_KEY_DURATION to "Duration",
    MediaMetadataRetriever.METADATA_KEY_TITLE to "Title",
    MediaMetadataRetriever.METADATA_KEY_ARTIST to "Artist",
    MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST to "Album Artist",
    MediaMetadataRetriever.METADATA_KEY_ALBUM to "Album",
    MediaMetadataRetriever.METADATA_KEY_COMPOSER to "Composer",
    MediaMetadataRetriever.METADATA_KEY_WRITER to "Writer",
    MediaMetadataRetriever.METADATA_KEY_GENRE to "Genre",
    MediaMetadataRetriever.METADATA_KEY_YEAR to "Year",
    MediaMetadataRetriever.METADATA_KEY_DATE to "Date",
    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "Track Number",
    MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER to "Disc Number",
    MediaMetadataRetriever.METADATA_KEY_COMPILATION to "Compilation",
    MediaMetadataRetriever.METADATA_KEY_BITRATE to "Bitrate",
    MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "Codec MIME",
    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "Video Width",
    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "Video Height",
    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "Rotation",
    MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE to "Frame Rate",
    MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "Tracks",
    MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO to "Has Audio",
    MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO to "Has Video",
    MediaMetadataRetriever.METADATA_KEY_LOCATION to "Location",
    MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD to "Color Standard",
    MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER to "Color Transfer",
    MediaMetadataRetriever.METADATA_KEY_COLOR_RANGE to "Color Range",
    MediaMetadataRetriever.METADATA_KEY_SAMPLERATE to "Sample Rate",
)

// Mapping from MediaMetadataRetriever keys to MediaStore column names for writing
private val MEDIA_KEY_TO_COLUMN = mapOf(
    MediaMetadataRetriever.METADATA_KEY_TITLE to MediaStore.MediaColumns.DISPLAY_NAME,
    MediaMetadataRetriever.METADATA_KEY_ARTIST to MediaStore.Audio.AudioColumns.ARTIST,
    MediaMetadataRetriever.METADATA_KEY_ALBUM to MediaStore.Audio.AudioColumns.ALBUM,
    MediaMetadataRetriever.METADATA_KEY_COMPOSER to MediaStore.Audio.AudioColumns.COMPOSER,
    MediaMetadataRetriever.METADATA_KEY_YEAR to MediaStore.Audio.AudioColumns.YEAR,
    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to MediaStore.Audio.AudioColumns.TRACK,
    MediaMetadataRetriever.METADATA_KEY_GENRE to MediaStore.Audio.AudioColumns.GENRE_ID,
)

// Common metadata fields for the help modal
private val COMMON_METADATA_FIELDS = listOf(
    "Title" to "The name or title of the content",
    "Author / Artist" to "The person who created the content",
    "Copyright" to "Copyright ownership information",
    "Description" to "A brief description of the content",
    "Version" to "Version number of the file or software",
    "Keywords / Tags" to "Searchable terms associated with the file",
    "Subject" to "The topic or subject of the content",
    "Creator / Software" to "The application used to create the file",
    "Date Created" to "When the file was originally created",
    "Date Modified" to "When the file was last changed",
    "GPS Coordinates" to "Geographic location where a photo was taken",
    "Camera Make / Model" to "The device used to capture an image",
    "Lens Make / Model" to "The lens used to capture an image",
    "Resolution / Dimensions" to "Width and height of image or video",
    "Duration" to "Length of audio or video content",
    "Bitrate" to "Data rate for audio or video encoding",
    "Scene / Capture Type" to "Scene mode used during capture",
    "Metering / Light" to "Metering mode and light source info",
)

@Composable
fun FileMetadataScreen() {
    val context = LocalContext.current
    val strings = S.fileMeta

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("") }
    var lastModified by remember { mutableStateOf("") }
    var generalMeta by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // EXIF now stored as tag->value map for inline editing
    var exifTagValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // Media metadata stored as key->value map for inline editing
    var mediaTagValues by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    // Read-only media metadata (non-editable fields)
    var mediaReadOnly by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isImage by remember { mutableStateOf(false) }
    var isMedia by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Take persistable permission for potential writes
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }

        selectedUri = uri
        val result = readFileMetadata(context, uri)
        fileName = result.name
        fileSize = result.size
        mimeType = result.mimeType
        lastModified = result.lastModified
        generalMeta = result.general
        exifTagValues = result.exifMap
        mediaTagValues = result.mediaMap
        mediaReadOnly = result.mediaReadOnly
        isImage = result.mimeType.startsWith("image/")
        isMedia = result.mimeType.startsWith("audio/") || result.mimeType.startsWith("video/")
    }

    ScreenAnnouncement(S.accessibility.fileMetaScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(
                Icons.Default.InsertDriveFile, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                strings.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── File Picker Button ────────────────────────────────────────
        Button(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text(strings.selectFile)
        }

        if (selectedUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        strings.noFileSelected,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        } else {
            // ── General Metadata ──────────────────────────────────────
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    MetaRow(strings.fileSize, fileSize)
                    MetaRow(strings.mimeType, mimeType)
                    if (lastModified.isNotBlank()) MetaRow(strings.dateModified, lastModified)
                }
            }

            // Additional general metadata
            if (generalMeta.isNotEmpty()) {
                generalMeta.forEach { (key, value) ->
                    MetaRow(key, value)
                }
            }

            // ── EXIF Data (Images) — inline editable ─────────────────
            if (isImage && exifTagValues.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    strings.editMetadata,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.sectionHeading(),
                )

                // Show all EXIF values; editable tags get OutlinedTextField, others get MetaRow
                ALL_EXIF_TAGS.forEach { (tag, label) ->
                    val currentValue = exifTagValues[tag] ?: return@forEach
                    if (tag in EDITABLE_TAG_KEYS) {
                        var fieldValue by remember(tag, currentValue) { mutableStateOf(currentValue) }
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = {
                                fieldValue = it
                                exifTagValues = exifTagValues.toMutableMap().apply { put(tag, it) }
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    } else {
                        MetaRow(label, currentValue)
                    }
                }

                Button(
                    onClick = {
                        // Write only editable tags
                        val editableData = exifTagValues.filterKeys { it in EDITABLE_TAG_KEYS }
                        val success = writeExifData(context, selectedUri!!, editableData)
                        Toast.makeText(
                            context,
                            if (success) strings.exifSaved else strings.exifSaveFailed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.saveChanges)
                }
            }

            // ── Media Metadata (Audio/Video) — inline editable ───────
            if (isMedia && (mediaTagValues.isNotEmpty() || mediaReadOnly.isNotEmpty())) {
                HorizontalDivider()
                Text(
                    strings.editMediaMetadata,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.sectionHeading(),
                )

                // Show editable media tags
                ALL_MEDIA_TAGS.forEach { (key, label) ->
                    val currentValue = mediaTagValues[key]
                    if (currentValue != null && key in EDITABLE_MEDIA_KEYS) {
                        var fieldValue by remember(key, currentValue) { mutableStateOf(currentValue) }
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = {
                                fieldValue = it
                                mediaTagValues = mediaTagValues.toMutableMap().apply { put(key, it) }
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                // Show read-only media metadata
                if (mediaReadOnly.isNotEmpty()) {
                    mediaReadOnly.forEach { (key, value) ->
                        MetaRow(key, value)
                    }
                }

                Button(
                    onClick = {
                        val editableData = mediaTagValues.filterKeys { it in EDITABLE_MEDIA_KEYS }
                        val success = writeMediaMetadata(context, selectedUri!!, editableData, mimeType)
                        Toast.makeText(
                            context,
                            if (success) strings.mediaSaved else strings.mediaSaveFailed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.saveChanges)
                }
            }

            // ── Add Metadata + Help ───────────────────────────────────
            if (isImage || isMedia) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addMetadata)
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, S.accessibility.help,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                // Non-image/media: just show help button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, S.accessibility.help,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    // ── Help Dialog ───────────────────────────────────────────────────
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.commonFields) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    COMMON_METADATA_FIELDS.forEach { (field, desc) ->
                        Column {
                            Text(field, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text(S.common.close) }
            },
        )
    }

    // ── Add Metadata Dialog ───────────────────────────────────────────
    if (showAddDialog) {
        if (isImage) {
            // Image: show available EXIF tags
            val availableTags = EDITABLE_EXIF_TAGS.filter { it.first !in exifTagValues }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(strings.addMetadata) },
                text = {
                    if (availableTags.isEmpty()) {
                        Text(strings.allFieldsPresent)
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            availableTags.forEach { (tag, label) ->
                                TextButton(
                                    onClick = {
                                        exifTagValues = exifTagValues.toMutableMap().apply { put(tag, "") }
                                        showAddDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(S.common.close) }
                },
            )
        } else if (isMedia) {
            // Audio/Video: show available media tags
            val availableTags = EDITABLE_MEDIA_TAGS.filter { it.first !in mediaTagValues }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(strings.addMetadata) },
                text = {
                    if (availableTags.isEmpty()) {
                        Text(strings.allFieldsPresent)
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            availableTags.forEach { (key, label) ->
                                TextButton(
                                    onClick = {
                                        mediaTagValues = mediaTagValues.toMutableMap().apply { put(key, "") }
                                        showAddDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(S.common.close) }
                },
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f),
        )
    }
}

// ── Metadata reading ─────────────────────────────────────────────────

private data class FileMetadataResult(
    val name: String,
    val size: String,
    val mimeType: String,
    val lastModified: String,
    val general: List<Pair<String, String>>,
    val exifMap: Map<String, String>,
    val mediaMap: Map<Int, String>,
    val mediaReadOnly: List<Pair<String, String>>,
)

private fun readFileMetadata(context: Context, uri: Uri): FileMetadataResult {
    var name = ""
    var size = ""
    var mimeTypeStr = context.contentResolver.getType(uri) ?: "unknown"
    var lastMod = ""
    val general = mutableListOf<Pair<String, String>>()
    val exifMap = mutableMapOf<String, String>()
    val mediaMap = mutableMapOf<Int, String>()
    val mediaReadOnly = mutableListOf<Pair<String, String>>()

    // Basic metadata from ContentResolver
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: ""
            if (sizeIdx >= 0) {
                val bytes = cursor.getLong(sizeIdx)
                size = formatFileSize(bytes)
            }
            // Try to get last modified
            val modIdx = cursor.getColumnIndex("last_modified")
            if (modIdx >= 0) {
                val ms = cursor.getLong(modIdx)
                if (ms > 0) lastMod = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
            }
        }
    }

    // Extension
    val ext = name.substringAfterLast('.', "").lowercase()
    if (ext.isNotBlank()) general.add("Extension" to ".$ext")

    // EXIF for images — read all tags into map
    if (mimeTypeStr.startsWith("image/")) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                ALL_EXIF_TAGS.forEach { (tag, _) ->
                    val value = exifInterface.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        exifMap[tag] = value
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Media metadata for audio/video — split into editable map + read-only list
    if (mimeTypeStr.startsWith("audio/") || mimeTypeStr.startsWith("video/")) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            ALL_MEDIA_TAGS.forEach { (key, label) ->
                val value = retriever.extractMetadata(key)
                if (!value.isNullOrBlank()) {
                    if (key in EDITABLE_MEDIA_KEYS) {
                        mediaMap[key] = value
                    } else {
                        val display = formatMediaValue(key, value)
                        mediaReadOnly.add(label to display)
                    }
                }
            }
            retriever.release()
        } catch (_: Exception) {}
    }

    return FileMetadataResult(name, size, mimeTypeStr, lastMod, general, exifMap, mediaMap, mediaReadOnly)
}

private fun formatMediaValue(key: Int, value: String): String = when (key) {
    MediaMetadataRetriever.METADATA_KEY_DURATION -> {
        val ms = value.toLongOrNull() ?: 0
        val sec = ms / 1000
        "%d:%02d".format(sec / 60, sec % 60)
    }
    MediaMetadataRetriever.METADATA_KEY_BITRATE -> {
        val bps = value.toLongOrNull() ?: 0
        "${bps / 1000} kbps"
    }
    MediaMetadataRetriever.METADATA_KEY_SAMPLERATE -> {
        val hz = value.toLongOrNull() ?: 0
        "${hz / 1000.0} kHz"
    }
    MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE -> "$value fps"
    MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO,
    MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO -> if (value == "yes") "Yes" else "No"
    else -> value
}

private fun writeExifData(context: Context, uri: Uri, data: Map<String, String>): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            val exif = ExifInterface(pfd.fileDescriptor)
            data.forEach { (tag, value) ->
                exif.setAttribute(tag, value.ifBlank { null })
            }
            exif.saveAttributes()
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun writeMediaMetadata(
    context: Context,
    uri: Uri,
    data: Map<Int, String>,
    mimeType: String,
): Boolean {
    return try {
        val values = ContentValues()
        data.forEach { (key, value) ->
            val column = MEDIA_KEY_TO_COLUMN[key]
            if (column != null && value.isNotBlank()) {
                values.put(column, value)
            }
        }
        if (values.size() > 0) {
            context.contentResolver.update(uri, values, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
}
