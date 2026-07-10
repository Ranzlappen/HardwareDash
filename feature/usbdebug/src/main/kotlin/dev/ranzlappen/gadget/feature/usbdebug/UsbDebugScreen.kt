package dev.ranzlappen.gadget.feature.usbdebug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType
import dev.ranzlappen.gadget.feature.usbdebug.monitor.UsbDebuggingMetricSource

@Composable
fun UsbDebugScreen(
    modifier: Modifier = Modifier,
    viewModel: UsbDebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Refresh the ADB_ENABLED readout on resume — the user's most likely
    // path to changing it is the "Open Developer options" deep link, which
    // backgrounds this screen rather than recreating it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshUsbDebuggingEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    UsbDebugScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        moduleInfo = usbDebugModuleInfo(state),
        modifier = modifier,
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = UsbDebuggingMetricSource.METRIC_KEY,
                title = stringResource(R.string.usbdebug_live_monitor_title),
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = UsbDebuggingMetricSource.METRIC_KEY,
                title = stringResource(R.string.usbdebug_monitor_title),
            )
        },
    )
}

@Composable
private fun usbDebugModuleInfo(state: UsbDebugState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.usbdebug_cap_debug_state_name),
            detail = stringResource(R.string.usbdebug_cap_debug_state_detail),
            status = {
                if (state.usbDebuggingEnabled) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.usbdebug_cap_debug_state_enabled),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.usbdebug_cap_debug_state_disabled),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.usbdebug_cap_function_switch_name),
            detail = stringResource(R.string.usbdebug_cap_function_switch_detail),
            status = { rootedCapabilityStatus(state.isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.usbdebug_cap_dump_usb_name),
            detail = stringResource(R.string.usbdebug_cap_dump_usb_detail),
            status = { rootedCapabilityStatus(state.isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.usbdebug_cap_dump_serial_name),
            detail = stringResource(R.string.usbdebug_cap_dump_serial_detail),
            status = { rootedCapabilityStatus(state.isRootedFlavor) },
        ),
        ModuleCapability(
            name = stringResource(R.string.usbdebug_cap_dump_debugfs_name),
            detail = stringResource(R.string.usbdebug_cap_dump_debugfs_detail),
            status = { rootedCapabilityStatus(state.isRootedFlavor) },
        ),
    ),
)

@Composable
private fun rootedCapabilityStatus(isRootedFlavor: Boolean): CapabilityStatus =
    if (isRootedFlavor) CapabilityStatus(
        kind = GadgetStatusKind.Success,
        message = stringResource(R.string.usbdebug_cap_rooted_active),
    ) else CapabilityStatus(
        kind = GadgetStatusKind.Warning,
        message = stringResource(R.string.usbdebug_cap_rooted_required),
    )

@Composable
internal fun UsbDebugScreenContent(
    state: UsbDebugState,
    onEvent: (UsbDebugUiEvent) -> Unit,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.usbdebug_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            UsbDebuggingStatusCard(state = state, onEvent = onEvent)
            if (state.isRootedFlavor) {
                UsbFunctionPickerCard(state = state, onEvent = onEvent)
                UsbDiagnosticsPanel(state = state, onEvent = onEvent)
            }
            liveMonitors()
            monitors()
        },
    )
}

@Composable
private fun UsbDebuggingStatusCard(
    state: UsbDebugState,
    onEvent: (UsbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.usbdebug_status_card_title),
        icon = Icons.Filled.Usb,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetChip(
                selected = state.usbDebuggingEnabled,
                onClick = {},
                enabled = false,
                label = stringResource(
                    if (state.usbDebuggingEnabled) {
                        R.string.usbdebug_status_chip_enabled
                    } else {
                        R.string.usbdebug_status_chip_disabled
                    },
                ),
            )
            GadgetSecondaryButton(
                onClick = { onEvent(UsbDebugUiEvent.OpenDeveloperOptions) },
                text = stringResource(R.string.usbdebug_open_developer_options),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsbFunctionPickerCard(
    state: UsbDebugState,
    onEvent: (UsbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.usbdebug_function_card_title),
        icon = Icons.Filled.Usb,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = if (state.appliedFunction != null) {
                    stringResource(
                        R.string.usbdebug_function_applied,
                        usbFunctionLabel(state.appliedFunction),
                    )
                } else {
                    stringResource(R.string.usbdebug_function_none_applied)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            state.priorFunction?.let { prior ->
                Text(
                    text = stringResource(R.string.usbdebug_function_prior, prior),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.functionSwitchError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                UsbFunctionType.entries.forEach { function ->
                    GadgetChip(
                        selected = state.appliedFunction == function,
                        enabled = !state.functionSwitchInFlight,
                        onClick = { onEvent(UsbDebugUiEvent.SelectFunction(function)) },
                        label = usbFunctionLabel(function),
                    )
                }
            }
        }
    }
}

@Composable
private fun usbFunctionLabel(function: UsbFunctionType): String = when (function) {
    UsbFunctionType.NONE -> stringResource(R.string.usbdebug_function_chip_none)
    UsbFunctionType.MTP -> stringResource(R.string.usbdebug_function_chip_mtp)
    UsbFunctionType.PTP -> stringResource(R.string.usbdebug_function_chip_ptp)
    UsbFunctionType.RNDIS -> stringResource(R.string.usbdebug_function_chip_rndis)
    UsbFunctionType.MIDI -> stringResource(R.string.usbdebug_function_chip_midi)
    UsbFunctionType.NCM -> stringResource(R.string.usbdebug_function_chip_ncm)
    UsbFunctionType.ACCESSORY -> stringResource(R.string.usbdebug_function_chip_accessory)
}

@Composable
private fun UsbDiagnosticsPanel(
    state: UsbDebugState,
    onEvent: (UsbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.usbdebug_diagnostics_panel_title),
        expanded = state.diagnosticsExpanded,
        onExpandedChange = { onEvent(UsbDebugUiEvent.DiagnosticsToggle) },
        icon = Icons.Filled.BugReport,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            UsbDumpSubSection(
                title = stringResource(R.string.usbdebug_diagnostics_usb_title),
                dump = state.usbDump,
                onRun = { onEvent(UsbDebugUiEvent.RunUsbDump) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UsbDumpSubSection(
                title = stringResource(R.string.usbdebug_diagnostics_serial_title),
                dump = state.serialServiceDump,
                onRun = { onEvent(UsbDebugUiEvent.RunSerialServiceDump) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UsbDumpSubSection(
                title = stringResource(R.string.usbdebug_diagnostics_debugfs_title),
                dump = state.debugfsDump,
                onRun = { onEvent(UsbDebugUiEvent.RunUsbDevicesDebugDump) },
            )
        }
    }
}

/**
 * One labelled sub-section of the "USB Diagnostics" panel — a `Run`
 * trigger, the [UsbDumpPanelState.source] the controller actually
 * answered with, and the tail-capped excerpt itself.
 */
@Composable
private fun UsbDumpSubSection(
    title: String,
    dump: UsbDumpPanelState,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GadgetTertiaryButton(
                onClick = onRun,
                text = stringResource(R.string.usbdebug_diagnostics_run),
                loading = dump.loading,
            )
        }
        dump.source?.let { source ->
            Text(
                text = stringResource(R.string.usbdebug_diagnostics_source, source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            dump.error != null -> Text(
                text = dump.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            dump.excerpt != null -> Text(
                text = dump.excerpt,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 12,
                overflow = TextOverflow.Ellipsis,
            )
            else -> Text(
                text = stringResource(R.string.usbdebug_diagnostics_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun UsbDebugScreenStandardPreview() = GadgetThemedPreview {
    UsbDebugScreenContent(
        state = UsbDebugState.Initial.copy(isRootedFlavor = false, usbDebuggingEnabled = true),
        onEvent = {},
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun UsbDebugScreenRootedPreview() = GadgetThemedPreview {
    UsbDebugScreenContent(
        state = UsbDebugState.Initial.copy(
            isRootedFlavor = true,
            usbDebuggingEnabled = false,
            appliedFunction = UsbFunctionType.MTP,
            priorFunction = "mtp",
            diagnosticsExpanded = true,
            usbDump = UsbDumpPanelState(
                excerpt = "Device: mtp\nState: CONFIGURED",
                source = "dumpsys usb",
            ),
        ),
        onEvent = {},
        moduleInfo = null,
    )
}
