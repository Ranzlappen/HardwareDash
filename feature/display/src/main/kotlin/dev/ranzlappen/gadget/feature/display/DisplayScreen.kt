package dev.ranzlappen.gadget.feature.display

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.OsNote
import dev.ranzlappen.gadget.core.ui.module.RootActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.display.monitor.DisplayBrightnessMetricSource
import kotlin.math.roundToInt

@Composable
fun DisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: DisplayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    DisplayScreenContent(
        state = state,
        moduleInfo = displayModuleInfo(
            state = state,
            onRequestWriteSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
        ),
        onEvent = viewModel::onEvent,
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.display_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.display_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.display_root_surfaceflinger_label),
                    description = stringResource(R.string.display_root_surfaceflinger_detail),
                    runLabel = stringResource(R.string.display_root_run),
                    onRun = viewModel::onDumpSurfaceFlinger,
                    enabled = !rootTools.surfaceFlinger.running,
                    statusMessage = rootTools.surfaceFlinger.message,
                    statusKind = rootTools.surfaceFlinger.statusKind,
                )
            }
        },
        monitors = {
            LiveMonitorContainer(
                metricKey = DisplayBrightnessMetricSource.METRIC_KEY,
                title = stringResource(R.string.display_live_monitor_brightness),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "display_live_${DisplayBrightnessMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = DisplayBrightnessMetricSource.METRIC_KEY,
                title = stringResource(R.string.display_monitor_brightness),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "display_monitor_${DisplayBrightnessMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun displayModuleInfo(
    state: DisplayState,
    onRequestWriteSettings: () -> Unit,
): ModuleInfo = ModuleInfo(
    permissions = listOf(
        ModulePermission(
            permission = "android.permission.WRITE_SETTINGS",
            label = stringResource(R.string.display_permission_write_settings_label),
            rationale = stringResource(R.string.display_permission_write_settings_rationale),
            optional = true,
        ),
    ),
    compatibility = OsCompatibility(
        minSdk = 1,
        notes = listOf(
            OsNote(30, stringResource(R.string.display_os_note_refresh_rate_api30)),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.display_cap_brightness_name),
            detail = stringResource(R.string.display_cap_brightness_detail),
            status = {
                if (state.brightnessWritable) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.display_cap_brightness_granted),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.display_cap_brightness_denied),
                        action = CapabilityAction.Custom(
                            label = stringResource(R.string.display_action_grant_write_settings),
                            onClick = onRequestWriteSettings,
                        ),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.display_cap_refresh_rate_name),
            detail = stringResource(R.string.display_cap_refresh_rate_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.display_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.display_cap_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.display_cap_density_name),
            detail = stringResource(R.string.display_cap_density_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.display_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.display_cap_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.display_cap_surfaceflinger_name),
            detail = stringResource(R.string.display_cap_surfaceflinger_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.display_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.display_cap_rooted_required),
                    )
                }
            },
        ),
    ),
)

@Composable
internal fun DisplayScreenContent(
    state: DisplayState,
    moduleInfo: ModuleInfo?,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
    rootTools: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.display_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            DisplayReadoutsCard(state = state)
            DisplayBrightnessCard(state = state, onEvent = onEvent)
            DisplayRefreshRateCard(state = state, onEvent = onEvent)
            DisplayDensityCard(state = state, onEvent = onEvent)
            DisplaySurfaceFlingerCard(state = state, onEvent = onEvent)
            DisplayResetCard(state = state, onEvent = onEvent)
            state.statusMessage?.let { DisplayStatusRow(message = it) }
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun DisplayReadoutsCard(state: DisplayState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_readouts_title),
        icon = Icons.Filled.DesktopWindows,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            ReadoutRow(
                icon = Icons.Filled.Speed,
                label = stringResource(R.string.display_readout_refresh_rate),
                value = stringResource(R.string.display_value_hz, state.refreshRateHz.roundToInt()),
            )
            ReadoutRow(
                icon = Icons.Filled.ScreenRotation,
                label = stringResource(R.string.display_readout_rotation),
                value = stringResource(R.string.display_value_degrees, state.rotationDegrees),
            )
            ReadoutRow(
                icon = Icons.Filled.AspectRatio,
                label = stringResource(R.string.display_readout_resolution),
                value = stringResource(
                    R.string.display_value_resolution,
                    state.resolutionWidth,
                    state.resolutionHeight,
                ),
            )
        }
    }
}

@Composable
private fun ReadoutRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DisplayBrightnessCard(
    state: DisplayState,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localPercent by remember(state.brightnessPercent) {
        mutableFloatStateOf(state.brightnessPercent.toFloat())
    }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_brightness_title),
        icon = Icons.Filled.BrightnessMedium,
    ) {
        Column {
            if (!state.brightnessWritable) {
                Text(
                    text = stringResource(R.string.display_cap_brightness_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            GadgetSlider(
                value = localPercent,
                onValueChange = { localPercent = it },
                onValueChangeFinished = { onEvent(DisplayUiEvent.BrightnessCommitted(localPercent.roundToInt())) },
                valueRange = 0f..100f,
                enabled = state.brightnessWritable,
                label = stringResource(R.string.display_brightness_label),
                suffix = "%",
            )
        }
    }
}

@Composable
private fun DisplayRefreshRateCard(
    state: DisplayState,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.availableRefreshRates.isEmpty()) return
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_refresh_rate_title),
        icon = Icons.Filled.Speed,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = if (state.isRootedFlavor) {
                    stringResource(R.string.display_refresh_rate_subtitle_rooted)
                } else {
                    stringResource(R.string.display_refresh_rate_subtitle_standard)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                items(state.availableRefreshRates) { option ->
                    GadgetChip(
                        selected = option.refreshRateHz == state.selectedRefreshRateHz,
                        onClick = { onEvent(DisplayUiEvent.RefreshRateSelected(option)) },
                        label = stringResource(R.string.display_value_hz, option.refreshRateHz.roundToInt()),
                        enabled = !state.isApplyingRefreshRate,
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayDensityCard(
    state: DisplayState,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localDpi by remember(state.densityDpi) { mutableFloatStateOf(state.densityDpi.toFloat()) }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_density_title),
        icon = Icons.Filled.AspectRatio,
    ) {
        Column {
            Text(
                text = if (state.isRootedFlavor) {
                    stringResource(R.string.display_density_subtitle_rooted)
                } else {
                    stringResource(R.string.display_density_subtitle_standard)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetSlider(
                value = localDpi,
                onValueChange = { localDpi = it },
                onValueChangeFinished = { onEvent(DisplayUiEvent.DensityCommitted(localDpi.roundToInt())) },
                valueRange = DisplayState.MIN_DENSITY_DPI.toFloat()..DisplayState.MAX_DENSITY_DPI.toFloat(),
                enabled = state.isRootedFlavor && !state.isApplyingDensity,
                label = stringResource(R.string.display_density_label),
                suffix = "dpi",
            )
        }
    }
}

@Composable
private fun DisplaySurfaceFlingerCard(
    state: DisplayState,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_surfaceflinger_title),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        icon = Icons.Filled.DesktopWindows,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = if (state.isRootedFlavor) {
                    stringResource(R.string.display_surfaceflinger_subtitle_rooted)
                } else {
                    stringResource(R.string.display_surfaceflinger_subtitle_standard)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetPrimaryButton(
                onClick = { onEvent(DisplayUiEvent.SurfaceFlingerSnapshotRequested) },
                text = stringResource(R.string.display_surfaceflinger_capture),
                enabled = state.isRootedFlavor && !state.isLoadingSurfaceFlinger,
                loading = state.isLoadingSurfaceFlinger,
            )
            state.surfaceFlingerExcerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DisplayResetCard(
    state: DisplayState,
    onEvent: (DisplayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.display_reset_title),
        icon = Icons.Filled.RestartAlt,
    ) {
        Column {
            Text(
                text = stringResource(R.string.display_reset_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetPrimaryButton(
                onClick = { onEvent(DisplayUiEvent.ResetAllRequested) },
                text = stringResource(R.string.display_reset_action),
                enabled = !state.isResetting,
                loading = state.isResetting,
            )
        }
    }
}

@Composable
private fun DisplayStatusRow(message: String, modifier: Modifier = Modifier) {
    DashCard(modifier = modifier.fillMaxWidth(), title = stringResource(R.string.display_status_title)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Previews ───────────────────────────────────────────────────────

private val previewRefreshRates = listOf(
    RefreshRateOption(modeId = 1, refreshRateHz = 60f),
    RefreshRateOption(modeId = 2, refreshRateHz = 90f),
    RefreshRateOption(modeId = 3, refreshRateHz = 120f),
)

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun DisplayScreenStandardPreview() = GadgetThemedPreview {
    DisplayScreenContent(
        state = DisplayState(
            isRootedFlavor = false,
            brightnessPercent = 65,
            brightnessWritable = true,
            refreshRateHz = 60f,
            availableRefreshRates = previewRefreshRates,
            selectedRefreshRateHz = 60f,
            rotationDegrees = 0,
            resolutionWidth = 1080,
            resolutionHeight = 2400,
        ),
        moduleInfo = null,
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun DisplayScreenRootedPreview() = GadgetThemedPreview {
    DisplayScreenContent(
        state = DisplayState(
            isRootedFlavor = true,
            brightnessPercent = 80,
            brightnessWritable = true,
            refreshRateHz = 120f,
            availableRefreshRates = previewRefreshRates,
            selectedRefreshRateHz = 120f,
            rotationDegrees = 90,
            resolutionWidth = 2400,
            resolutionHeight = 1080,
            densityDpi = 440,
            surfaceFlingerExcerpt = "HWC layers: 4\nVSYNC: 120.0fps\n...",
        ),
        moduleInfo = null,
        onEvent = {},
    )
}
