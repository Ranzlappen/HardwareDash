package dev.ranzlappen.gadget.feature.files

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import java.util.Date

/** Hilt entry for the Files tool (v3 B12). */
@Composable
fun FilesScreen(
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // OpenDocument grants read and, where the provider supports it, write.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::open)
    }
    FilesScreenContent(
        state = state,
        modifier = modifier,
        onPick = { picker.launch(arrayOf("*/*")) },
        onEdit = viewModel::edit,
        onSave = viewModel::save,
        onRevert = viewModel::revert,
        onRemoveLocation = viewModel::removeLocation,
    )
}

@Composable
internal fun FilesScreenContent(
    state: FilesState,
    modifier: Modifier = Modifier,
    onPick: () -> Unit = {},
    onEdit: (String, String) -> Unit = { _, _ -> },
    onSave: () -> Unit = {},
    onRevert: () -> Unit = {},
    onRemoveLocation: () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.files_title),
        modifier = modifier,
        moduleInfo = ModuleInfo(compatibility = OsCompatibility(minSdk = 29)),
        functional = {
            PickCard(state = state, onPick = onPick)
            state.metadata?.let { meta ->
                GeneralCard(meta)
                if (meta.mimeType?.startsWith("image/") == true) {
                    ExifCard(state, meta, onEdit, onSave, onRevert, onRemoveLocation)
                    CameraCard(meta)
                }
                meta.media?.let { MediaCard(it) }
            }
        },
    )
}

@Composable
private fun PickCard(state: FilesState, onPick: () -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.files_title),
        icon = Icons.Filled.FolderOpen,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            val hint = when {
                state.loading -> stringResource(R.string.files_loading)
                state.message == FilesMessage.ReadFailed -> stringResource(R.string.files_read_failed)
                state.metadata == null -> stringResource(R.string.files_empty)
                else -> null
            }
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GadgetPrimaryButton(
                onClick = onPick,
                text = stringResource(R.string.files_pick),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )
        }
    }
}

@Composable
private fun GeneralCard(meta: FileMetadata) {
    val context = LocalContext.current
    val spacing = LocalGadgetTheme.current.spacing
    val unknown = stringResource(R.string.files_unknown)
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = meta.name,
        icon = Icons.Filled.Description,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            InfoRow(stringResource(R.string.files_size), meta.sizeBytes?.let { Formatter.formatFileSize(context, it) } ?: unknown)
            InfoRow(stringResource(R.string.files_type), meta.mimeType ?: unknown)
            meta.extension?.let { InfoRow(stringResource(R.string.files_extension), ".$it") }
            meta.modifiedMs?.let {
                InfoRow(
                    stringResource(R.string.files_modified),
                    DateFormat.getMediumDateFormat(context).format(Date(it)) + " " +
                        DateFormat.getTimeFormat(context).format(Date(it)),
                )
            }
            meta.dimensions?.let { InfoRow(stringResource(R.string.files_dimensions), it) }
        }
    }
}

@Composable
private fun ExifCard(
    state: FilesState,
    meta: FileMetadata,
    onEdit: (String, String) -> Unit,
    onSave: () -> Unit,
    onRevert: () -> Unit,
    onRemoveLocation: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var confirmRemove by remember { mutableStateOf(false) }
    val writable = meta.exifWritable
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.files_exif_title),
        icon = Icons.Filled.Photo,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (meta.exif == null) {
                Note(stringResource(R.string.files_exif_none))
                return@Column
            }
            if (!writable) Note(stringResource(R.string.files_exif_read_only_note))
            FileMetadataFormat.EDITABLE_TAGS.forEach { tag ->
                GadgetTextField(
                    value = state.edits[tag].orEmpty(),
                    onValueChange = { onEdit(tag, it) },
                    label = stringResource(editableLabel(tag)),
                    enabled = writable && !state.saving,
                    singleLine = tag != ExifInterface.TAG_USER_COMMENT && tag != ExifInterface.TAG_IMAGE_DESCRIPTION,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (writable) {
                GadgetPrimaryButton(
                    onClick = onSave,
                    text = stringResource(if (state.saving) R.string.files_saving else R.string.files_save),
                    enabled = state.changes.isNotEmpty() && !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.changes.isNotEmpty()) {
                    GadgetTertiaryButton(onClick = onRevert, text = stringResource(R.string.files_revert))
                }
            }
            when (state.message) {
                FilesMessage.Saved -> Note(stringResource(R.string.files_saved), MaterialTheme.colorScheme.primary)
                FilesMessage.LocationRemoved -> Note(stringResource(R.string.files_location_removed), MaterialTheme.colorScheme.primary)
                FilesMessage.SaveFailed -> Note(stringResource(R.string.files_save_failed), MaterialTheme.colorScheme.error)
                else -> Unit
            }
            Text(stringResource(R.string.files_location), style = MaterialTheme.typography.titleSmall)
            val location = meta.location
            Note(
                location?.let { FileMetadataFormat.formatLocation(it.first, it.second) }
                    ?: stringResource(R.string.files_location_none),
                if (location != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (location != null && writable) {
                GadgetSecondaryButton(
                    onClick = { confirmRemove = true },
                    text = stringResource(R.string.files_remove_location),
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (confirmRemove) {
        GadgetDialog(
            onDismissRequest = { confirmRemove = false },
            title = stringResource(R.string.files_remove_location_title),
            text = stringResource(R.string.files_remove_location_body),
            icon = Icons.Filled.LocationOff,
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = { confirmRemove = false; onRemoveLocation() },
                    text = stringResource(R.string.files_remove_location),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(onClick = { confirmRemove = false }, text = stringResource(R.string.files_cancel))
            },
        )
    }
}

@Composable
private fun CameraCard(meta: FileMetadata) {
    val exif = meta.exif ?: return
    val rows = FileMetadataFormat.READ_ONLY_TAGS.mapNotNull { tag ->
        exif[tag]?.let { MetaEntry(tag, FileMetadataFormat.formatExif(tag, it)) }
    }
    if (rows.isEmpty()) return
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.files_camera_title),
        icon = Icons.Filled.CameraAlt,
    ) {
        // EXIF tag names are the standard's own identifiers; shown untranslated.
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            rows.forEach { InfoRow(it.label, it.value) }
        }
    }
}

@Composable
private fun MediaCard(entries: List<MediaEntry>) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.files_media_title),
        icon = Icons.Filled.Movie,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            Note(stringResource(R.string.files_media_read_only))
            if (entries.isEmpty()) Note(stringResource(R.string.files_media_none))
            entries.forEach { InfoRow(stringResource(it.field.labelRes()), it.value) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

private fun editableLabel(tag: String): Int = when (tag) {
    ExifInterface.TAG_IMAGE_DESCRIPTION -> R.string.files_exif_description
    ExifInterface.TAG_ARTIST -> R.string.files_exif_artist
    ExifInterface.TAG_COPYRIGHT -> R.string.files_exif_copyright
    ExifInterface.TAG_USER_COMMENT -> R.string.files_exif_comment
    ExifInterface.TAG_DATETIME_ORIGINAL -> R.string.files_exif_date_taken
    ExifInterface.TAG_MAKE -> R.string.files_exif_make
    ExifInterface.TAG_MODEL -> R.string.files_exif_model
    else -> R.string.files_exif_software
}

private fun MediaField.labelRes(): Int = when (this) {
    MediaField.Title -> R.string.files_media_field_title
    MediaField.Artist -> R.string.files_media_field_artist
    MediaField.Album -> R.string.files_media_field_album
    MediaField.AlbumArtist -> R.string.files_media_field_album_artist
    MediaField.Genre -> R.string.files_media_field_genre
    MediaField.Year -> R.string.files_media_field_year
    MediaField.Track -> R.string.files_media_field_track
    MediaField.Duration -> R.string.files_media_field_duration
    MediaField.Bitrate -> R.string.files_media_field_bitrate
    MediaField.SampleRate -> R.string.files_media_field_sample_rate
    MediaField.Resolution -> R.string.files_media_field_resolution
    MediaField.Rotation -> R.string.files_media_field_rotation
    MediaField.FrameRate -> R.string.files_media_field_frame_rate
    MediaField.Location -> R.string.files_media_field_location
    MediaField.Tracks -> R.string.files_media_field_tracks
    MediaField.Container -> R.string.files_media_field_container
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun FilesScreenEmptyPreview() = GadgetThemedPreview {
    FilesScreenContent(state = FilesState())
}

@GadgetPreviewLightDark
@Composable
private fun FilesScreenPhotoPreview() = GadgetThemedPreview {
    val exif = mapOf(
        ExifInterface.TAG_MAKE to "Xiaomi",
        ExifInterface.TAG_MODEL to "24069PC21G",
        ExifInterface.TAG_F_NUMBER to "1.7",
        ExifInterface.TAG_EXPOSURE_TIME to "0.008",
    )
    FilesScreenContent(
        state = FilesState(
            metadata = FileMetadata(
                name = "IMG_20261121_084500.jpg",
                sizeBytes = 4_200_000,
                mimeType = "image/jpeg",
                modifiedMs = 0,
                dimensions = "4080 × 3072",
                exif = exif,
                location = 52.52 to 13.405,
            ),
            edits = FileMetadataFormat.EDITABLE_TAGS.associateWith { exif[it].orEmpty() },
        ),
    )
}
