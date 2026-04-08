package com.hardwaredash.ui.screens

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import com.hardwaredash.localization.S
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Common EXIF tags that can be edited
private val EDITABLE_EXIF_TAGS = listOf(
    ExifInterface.TAG_ARTIST to "Artist",
    ExifInterface.TAG_COPYRIGHT to "Copyright",
    ExifInterface.TAG_IMAGE_DESCRIPTION to "Description",
    ExifInterface.TAG_SOFTWARE to "Software",
    ExifInterface.TAG_MAKE to "Camera Make",
    ExifInterface.TAG_MODEL to "Camera Model",
    ExifInterface.TAG_USER_COMMENT to "User Comment",
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
    "Resolution / Dimensions" to "Width and height of image or video",
    "Duration" to "Length of audio or video content",
    "Bitrate" to "Data rate for audio or video encoding",
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
    var exifData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var mediaMeta by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isImage by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Editable EXIF state
    var editableExif by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

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
        exifData = result.exif
        mediaMeta = result.media
        isImage = result.mimeType.startsWith("image/")
        editableExif = result.editableExif
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
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

            // ── EXIF Data (Images) ────────────────────────────────────
            if (exifData.isNotEmpty()) {
                HorizontalDivider()
                Text("EXIF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                exifData.forEach { (key, value) ->
                    MetaRow(key, value)
                }
            }

            // ── Media Metadata (Audio/Video) ──────────────────────────
            if (mediaMeta.isNotEmpty()) {
                HorizontalDivider()
                Text("Media", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                mediaMeta.forEach { (key, value) ->
                    MetaRow(key, value)
                }
            }

            // ── Edit EXIF (Images only) ───────────────────────────────
            if (isImage && editableExif.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    strings.editMetadata,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                editableExif.forEach { (tag, value) ->
                    val label = EDITABLE_EXIF_TAGS.firstOrNull { it.first == tag }?.second ?: tag
                    var fieldValue by remember(tag, value) { mutableStateOf(value) }
                    OutlinedTextField(
                        value = fieldValue,
                        onValueChange = {
                            fieldValue = it
                            editableExif = editableExif.toMutableMap().apply { put(tag, it) }
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                Button(
                    onClick = {
                        val success = writeExifData(context, selectedUri!!, editableExif)
                        Toast.makeText(
                            context,
                            if (success) "EXIF data saved" else "Failed to save EXIF data",
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
            if (isImage) {
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
                            Icons.Default.HelpOutline, null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                // Non-image: just show help button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, null,
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        val availableTags = EDITABLE_EXIF_TAGS.filter { it.first !in editableExif }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addMetadata) },
            text = {
                if (availableTags.isEmpty()) {
                    Text("All common fields are already present.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableTags.forEach { (tag, label) ->
                            TextButton(
                                onClick = {
                                    editableExif = editableExif.toMutableMap().apply { put(tag, "") }
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
    val exif: List<Pair<String, String>>,
    val media: List<Pair<String, String>>,
    val editableExif: Map<String, String>,
)

private fun readFileMetadata(context: Context, uri: Uri): FileMetadataResult {
    var name = ""
    var size = ""
    var mimeTypeStr = context.contentResolver.getType(uri) ?: "unknown"
    var lastMod = ""
    val general = mutableListOf<Pair<String, String>>()
    val exif = mutableListOf<Pair<String, String>>()
    val media = mutableListOf<Pair<String, String>>()
    val editableExif = mutableMapOf<String, String>()

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

    // EXIF for images
    if (mimeTypeStr.startsWith("image/")) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                val tags = listOf(
                    ExifInterface.TAG_IMAGE_WIDTH to "Width",
                    ExifInterface.TAG_IMAGE_LENGTH to "Height",
                    ExifInterface.TAG_ORIENTATION to "Orientation",
                    ExifInterface.TAG_DATETIME to "Date/Time",
                    ExifInterface.TAG_MAKE to "Camera Make",
                    ExifInterface.TAG_MODEL to "Camera Model",
                    ExifInterface.TAG_F_NUMBER to "F-Number",
                    ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
                    ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO",
                    ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
                    ExifInterface.TAG_FLASH to "Flash",
                    ExifInterface.TAG_WHITE_BALANCE to "White Balance",
                    ExifInterface.TAG_GPS_LATITUDE to "GPS Latitude",
                    ExifInterface.TAG_GPS_LONGITUDE to "GPS Longitude",
                    ExifInterface.TAG_GPS_ALTITUDE to "GPS Altitude",
                    ExifInterface.TAG_ARTIST to "Artist",
                    ExifInterface.TAG_COPYRIGHT to "Copyright",
                    ExifInterface.TAG_IMAGE_DESCRIPTION to "Description",
                    ExifInterface.TAG_SOFTWARE to "Software",
                    ExifInterface.TAG_USER_COMMENT to "User Comment",
                )
                tags.forEach { (tag, label) ->
                    val value = exifInterface.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        exif.add(label to value)
                    }
                }
                // Populate editable fields
                EDITABLE_EXIF_TAGS.forEach { (tag, _) ->
                    val value = exifInterface.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        editableExif[tag] = value
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Media metadata for audio/video
    if (mimeTypeStr.startsWith("audio/") || mimeTypeStr.startsWith("video/")) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val keys = listOf(
                MediaMetadataRetriever.METADATA_KEY_DURATION to "Duration",
                MediaMetadataRetriever.METADATA_KEY_TITLE to "Title",
                MediaMetadataRetriever.METADATA_KEY_ARTIST to "Artist",
                MediaMetadataRetriever.METADATA_KEY_ALBUM to "Album",
                MediaMetadataRetriever.METADATA_KEY_GENRE to "Genre",
                MediaMetadataRetriever.METADATA_KEY_YEAR to "Year",
                MediaMetadataRetriever.METADATA_KEY_BITRATE to "Bitrate",
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "Codec MIME",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "Video Width",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "Video Height",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "Rotation",
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "Tracks",
            )
            keys.forEach { (key, label) ->
                val value = retriever.extractMetadata(key)
                if (!value.isNullOrBlank()) {
                    val display = when (key) {
                        MediaMetadataRetriever.METADATA_KEY_DURATION -> {
                            val ms = value.toLongOrNull() ?: 0
                            val sec = ms / 1000
                            "%d:%02d".format(sec / 60, sec % 60)
                        }
                        MediaMetadataRetriever.METADATA_KEY_BITRATE -> {
                            val bps = value.toLongOrNull() ?: 0
                            "${bps / 1000} kbps"
                        }
                        else -> value
                    }
                    media.add(label to display)
                }
            }
            retriever.release()
        } catch (_: Exception) {}
    }

    return FileMetadataResult(name, size, mimeTypeStr, lastMod, general, exif, media, editableExif)
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

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
}
