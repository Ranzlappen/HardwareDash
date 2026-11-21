package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlinx.coroutines.delay

/**
 * Hilt entry point for the Audio feature screen. Wires the permission
 * launcher (stateful, needs a Compose context) and forwards state to the
 * stateless [AudioScreenContent].
 */
@Composable
fun AudioScreen(
    modifier: Modifier = Modifier,
    onOpenMicrophoneTools: () -> Unit = {},
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    // Lifecycle-aware: the mic closes when the app goes to the background.
    val live by viewModel.live.collectAsStateWithLifecycle()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    AudioScreenContent(
        state = state,
        live = live,
        modifier = modifier,
        onGrantPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onStartRecording = { viewModel.startRecording() },
        onStopRecording = { viewModel.stopRecording() },
        onTogglePlayback = viewModel::togglePlayback,
        onShareRecording = { shareRecording(context, it.uri) },
        onDeleteRecording = viewModel::deleteRecording,
        onDismissError = viewModel::clearError,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.audio_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.audio_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.audio_root_policy_label),
                    description = stringResource(R.string.audio_root_policy_detail),
                    runLabel = stringResource(R.string.audio_root_run),
                    onRun = viewModel::onDumpAudioPolicy,
                    enabled = !rootTools.audioPolicy.running,
                    statusMessage = rootTools.audioPolicy.message,
                    statusKind = rootTools.audioPolicy.statusKind,
                )
                // v3 B7b: the former top-level "Mic Tools" screen lives here.
                RootActionRow(
                    label = stringResource(R.string.audio_root_mic_tools_label),
                    description = stringResource(R.string.audio_root_mic_tools_detail),
                    runLabel = stringResource(R.string.audio_root_open),
                    onRun = onOpenMicrophoneTools,
                )
            }
        },
        monitors = {
            MonitorContainer(
                metricKey = DbMeterMetricSource.METRIC_KEY,
                title = stringResource(R.string.audio_monitor_title),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "audio_monitor_db_meter",
            )
        },
    )
}

private fun shareRecording(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("audio/wav")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Stateless Audio screen content (v3 B10): permission card or live level +
 * spectrum, the recorder, the recordings list, monitor history, root tools.
 */
@Composable
internal fun AudioScreenContent(
    state: AudioState,
    live: MicFrame?,
    modifier: Modifier = Modifier,
    onGrantPermission: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onTogglePlayback: (Recording) -> Unit = {},
    onShareRecording: (Recording) -> Unit = {},
    onDeleteRecording: (Recording) -> Unit = {},
    onDismissError: () -> Unit = {},
    rootTools: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.audio_capability_mic),
        modifier = modifier,
        moduleInfo = audioModuleInfo(state),
        functional = {
            if (!state.permissionGranted) {
                AudioPermissionCard(onGrantPermission = onGrantPermission)
            } else {
                SpectrumCard(frame = live)
            }
            VoiceRecordCard(
                state = state,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
            )
            RecordingsCard(
                state = state,
                onTogglePlayback = onTogglePlayback,
                onShare = onShareRecording,
                onDelete = onDeleteRecording,
                onDismissError = onDismissError,
            )
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun audioModuleInfo(state: AudioState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 29),
    permissions = listOf(
        ModulePermission(
            permission = Manifest.permission.RECORD_AUDIO,
            label = stringResource(R.string.audio_capability_mic),
            rationale = stringResource(R.string.audio_permission_body),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.audio_capability_mic),
            detail = stringResource(R.string.audio_capability_mic_detail),
            status = {
                if (state.permissionGranted) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.audio_capability_available),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.audio_capability_permission_required),
                    )
                }
            },
        ),
    ),
)

@Composable
private fun AudioPermissionCard(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_permission_card_title),
        icon = Icons.Filled.Mic,
    ) {
        Text(
            text = stringResource(R.string.audio_permission_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        GadgetPrimaryButton(
            onClick = onGrantPermission,
            text = stringResource(R.string.audio_permission_grant),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SpectrumCard(
    frame: MicFrame?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_spectrum_title),
        icon = Icons.Filled.GraphicEq,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = if (frame != null) {
                        "${frame.db.toInt()} ${stringResource(R.string.audio_db_unit)}"
                    } else {
                        stringResource(R.string.audio_spectrum_listening)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (frame != null) {
                    Text(
                        text = stringResource(R.string.audio_spectrum_peak, SpectrumMath.formatHz(frame.peakHz)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SpectrumBars(
                bands = frame?.bands ?: emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SPECTRUM_ASPECT),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(MicAnalyzer.MIN_HZ, 1_000f, MicAnalyzer.MAX_HZ).forEach { hz ->
                    Text(
                        text = SpectrumMath.formatHz(hz),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpectrumBars(
    bands: List<Float>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gap = LocalGadgetTheme.current.spacing.pico
    Canvas(modifier = modifier) {
        val count = MicAnalyzer.BAND_COUNT
        val gapPx = gap.toPx()
        val barWidth = (size.width - gapPx * (count - 1)) / count
        val radius = CornerRadius(barWidth / 4f, barWidth / 4f)
        for (i in 0 until count) {
            val x = i * (barWidth + gapPx)
            drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, size.height), radius)
            val level = bands.getOrNull(i) ?: 0f
            if (level > 0f) {
                val h = size.height * level
                drawRoundRect(barColor, Offset(x, size.height - h), Size(barWidth, h), radius)
            }
        }
    }
}

@Composable
private fun VoiceRecordCard(
    state: AudioState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isRecording) {
        elapsedMs = 0L
        while (state.isRecording) {
            delay(ELAPSED_TICK_MS)
            elapsedMs += ELAPSED_TICK_MS
        }
    }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_record_card_title),
        icon = Icons.Filled.Mic,
    ) {
        if (state.isRecording) {
            Text(
                text = stringResource(R.string.audio_record_elapsed, formatDuration(elapsedMs)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            GadgetSecondaryButton(
                onClick = onStopRecording,
                text = stringResource(R.string.audio_record_stop),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = stringResource(R.string.audio_record_idle),
                style = MaterialTheme.typography.bodyMedium,
            )
            GadgetPrimaryButton(
                onClick = onStartRecording,
                text = stringResource(R.string.audio_record_start),
                modifier = Modifier.fillMaxWidth(),
                enabled = state.permissionGranted,
            )
        }
    }
}

@Composable
private fun RecordingsCard(
    state: AudioState,
    onTogglePlayback: (Recording) -> Unit,
    onShare: (Recording) -> Unit,
    onDelete: (Recording) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var pendingDelete by remember { mutableStateOf<Recording?>(null) }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_recordings_title),
        icon = Icons.Filled.LibraryMusic,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (state.error == AudioViewModel.DELETE_FAILED) {
                Text(
                    text = stringResource(R.string.audio_recording_delete_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.recordings.isEmpty()) {
                Text(
                    text = stringResource(R.string.audio_recordings_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.recordings.forEach { recording ->
                RecordingRow(
                    recording = recording,
                    isPlaying = state.playingUri == recording.uri,
                    onTogglePlayback = { onDismissError(); onTogglePlayback(recording) },
                    onShare = { onShare(recording) },
                    onDelete = { pendingDelete = recording },
                )
            }
        }
    }
    pendingDelete?.let { recording ->
        GadgetDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.audio_recording_delete_title),
            text = stringResource(R.string.audio_recording_delete_body, recording.name),
            icon = Icons.Filled.Delete,
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = { pendingDelete = null; onDelete(recording) },
                    text = stringResource(R.string.audio_recording_delete),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { pendingDelete = null },
                    text = stringResource(R.string.audio_cancel),
                )
            },
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GadgetIconButton(
            onClick = onTogglePlayback,
            icon = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = stringResource(
                if (isPlaying) R.string.audio_recording_stop_playback else R.string.audio_recording_play,
            ),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.audio_recording_meta,
                    formatDuration(recording.durationMs),
                    Formatter.formatShortFileSize(context, recording.sizeBytes),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GadgetIconButton(
            onClick = onShare,
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.audio_record_share),
        )
        GadgetIconButton(
            onClick = onDelete,
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.audio_recording_delete),
        )
    }
}

private const val SPECTRUM_ASPECT = 2.5f
private const val ELAPSED_TICK_MS = 1_000L

// ─── Previews ───────────────────────────────────────────────────────

private val previewFrame = MicFrame(
    db = 38f,
    bands = List(MicAnalyzer.BAND_COUNT) { i -> (0.85f - i * 0.022f).coerceAtLeast(0.1f) },
    peakHz = 440f,
)

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun AudioScreenPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(permissionGranted = true),
        live = previewFrame,
    )
}

@GadgetPreviewLightDark
@Composable
private fun AudioScreenNoPermissionPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(permissionGranted = false),
        live = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun AudioScreenRecordingPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(permissionGranted = true, isRecording = true),
        live = previewFrame,
    )
}
