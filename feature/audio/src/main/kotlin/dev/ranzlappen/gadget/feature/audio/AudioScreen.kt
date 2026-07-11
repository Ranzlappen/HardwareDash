package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
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

/**
 * Hilt entry point for the Audio feature screen. Wires the permission
 * launcher (stateful, needs a Compose context) and forwards state to the
 * stateless [AudioScreenContent].
 */
@Composable
fun AudioScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    AudioScreenContent(
        state = state,
        modifier = modifier,
        onGrantPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onStartRecording = { viewModel.startRecording() },
        onStopRecording = { viewModel.stopRecording() },
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
            }
        },
        liveMonitors = {
            if (state.permissionGranted) {
                LiveMonitorContainer(
                    metricKey = DbMeterMetricSource.METRIC_KEY,
                    title = stringResource(R.string.audio_live_monitor_title),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "audio_live_monitor_db_meter",
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

/**
 * Stateless Audio screen content. Renders:
 *  - A permission card when [AudioState.permissionGranted] is false.
 *  - A live dB meter card when permission is granted.
 *  - A voice recording card.
 *  - Monitor history containers (supplied as a slot).
 */
@Composable
internal fun AudioScreenContent(
    state: AudioState,
    modifier: Modifier = Modifier,
    onGrantPermission: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    rootTools: @Composable () -> Unit = {},
    liveMonitors: @Composable () -> Unit = {},
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
                DbMeterCard(state = state)
            }
            VoiceRecordCard(
                state = state,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
            )
            liveMonitors()
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun audioModuleInfo(state: AudioState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 16),
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
        ModuleCapability(
            name = stringResource(R.string.audio_capability_gain_boost),
            detail = stringResource(R.string.audio_capability_gain_boost_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.audio_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.audio_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.audio_capability_direct_pcm),
            detail = stringResource(R.string.audio_capability_direct_pcm_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.audio_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.audio_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.audio_capability_custom_sample_rate),
            detail = stringResource(R.string.audio_capability_custom_sample_rate_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.audio_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.audio_capability_rooted_required),
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
private fun DbMeterCard(
    state: AudioState,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_db_meter_card_title),
        icon = Icons.Filled.Mic,
    ) {
        Text(
            text = stringResource(R.string.audio_db_level),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "${state.currentDbLevel.toInt()} ${stringResource(R.string.audio_db_unit)}",
            style = MaterialTheme.typography.headlineMedium,
        )
        LinearProgressIndicator(
            progress = { state.currentDbLevel / 60f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VoiceRecordCard(
    state: AudioState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.audio_record_card_title),
        icon = Icons.Filled.Mic,
    ) {
        if (state.isRecording) {
            Text(
                text = stringResource(R.string.audio_record_stop),
                style = MaterialTheme.typography.bodyMedium,
            )
            GadgetSecondaryButton(
                onClick = onStopRecording,
                text = stringResource(R.string.audio_record_stop),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = if (state.lastRecordingUri != null) {
                    stringResource(R.string.audio_record_saved)
                } else {
                    stringResource(R.string.audio_record_idle)
                },
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

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun AudioScreenPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(
            permissionGranted = true,
            currentDbLevel = 32f,
        ),
    )
}

@GadgetPreviewLightDark
@Composable
private fun AudioScreenNoPermissionPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(permissionGranted = false),
    )
}

@GadgetPreviewLightDark
@Composable
private fun AudioScreenRecordingPreview() = GadgetThemedPreview {
    AudioScreenContent(
        state = AudioState(
            permissionGranted = true,
            isRecording = true,
            currentDbLevel = 45f,
        ),
    )
}
