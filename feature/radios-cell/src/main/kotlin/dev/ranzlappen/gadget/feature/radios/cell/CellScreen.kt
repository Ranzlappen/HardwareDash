package dev.ranzlappen.gadget.feature.radios.cell

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
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
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Hilt entry point: collects the tracker-backed [CellState] + the two
 * on-demand rooted dump states, drives the `READ_PHONE_STATE` permission
 * request flow (accompanist-permissions, the same pattern
 * `:feature:gps`'s `GpsScreen` uses for `ACCESS_FINE_LOCATION`), and
 * supplies the monitor containers as a slot — keeping [CellScreenContent]
 * (and its previews/tests) Hilt-free.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CellScreen(
    modifier: Modifier = Modifier,
    viewModel: CellViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rawModemDump by viewModel.rawModemDump.collectAsStateWithLifecycle()
    val signalDeepDump by viewModel.signalDeepDump.collectAsStateWithLifecycle()
    val rootTools by viewModel.rootTools.collectAsStateWithLifecycle()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val isRootedFlavor = viewModel.isRootedFlavor
    val permissionState = rememberPermissionState(Manifest.permission.READ_PHONE_STATE)

    // Start/stop the TelephonyManager listener as the grant state changes —
    // mirrors GpsScreen's onPermissionGranted/onPermissionRevoked pairing.
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) viewModel.onPermissionGranted()
        else viewModel.onPermissionRevoked()
    }

    // Re-check on every ON_RESUME so returning from the system permission
    // (or app-settings) screen refreshes the tracker without a manual pull.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (permissionState.status.isGranted) viewModel.onPermissionGranted()
                else viewModel.onPermissionRevoked()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CellScreenContent(
        state = state,
        isRootedFlavor = isRootedFlavor,
        moduleInfo = cellModuleInfo(
            state = state,
            permissionGranted = permissionState.status.isGranted,
            isRootedFlavor = isRootedFlavor,
        ),
        rawModemDump = rawModemDump,
        signalDeepDump = signalDeepDump,
        onRequestPermission = { permissionState.launchPermissionRequest() },
        onLoadRawModemDump = viewModel::loadRawModemDump,
        onLoadSignalDeepDump = viewModel::loadSignalDeepDump,
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.cell_root_tools_title),
                available = isRootedFlavor,
                unavailableMessage = stringResource(R.string.cell_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.cell_root_modem_label),
                    description = stringResource(R.string.cell_root_modem_detail),
                    runLabel = stringResource(R.string.cell_root_run),
                    onRun = viewModel::onDumpModem,
                    enabled = !rootTools.modem.running,
                    statusMessage = rootTools.modem.message,
                    statusKind = rootTools.modem.statusKind,
                )
                RootActionRow(
                    label = stringResource(R.string.cell_root_signal_label),
                    description = stringResource(R.string.cell_root_signal_detail),
                    runLabel = stringResource(R.string.cell_root_run),
                    onRun = viewModel::onDumpSignal,
                    enabled = !rootTools.signal.running,
                    statusMessage = rootTools.signal.message,
                    statusKind = rootTools.signal.statusKind,
                )
            }
        },
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = CellSignalMetricSource.METRIC_KEY,
                title = stringResource(R.string.cell_live_monitor_signal),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "cell_live_${CellSignalMetricSource.METRIC_KEY}",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = CellSignalMetricSource.METRIC_KEY,
                title = stringResource(R.string.cell_monitor_signal),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "cell_monitor_${CellSignalMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun cellModuleInfo(
    state: CellState,
    permissionGranted: Boolean,
    isRootedFlavor: Boolean,
): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 29),
    permissions = listOf(
        ModulePermission(
            permission = Manifest.permission.READ_PHONE_STATE,
            label = stringResource(R.string.cell_permission_label),
            rationale = stringResource(R.string.cell_permission_rationale),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.cell_cap_sim_name),
            detail = stringResource(R.string.cell_cap_sim_detail),
            status = { simStateCapabilityStatus(state.simState) },
        ),
        ModuleCapability(
            name = stringResource(R.string.cell_cap_carrier_name),
            detail = stringResource(R.string.cell_cap_carrier_detail),
            status = {
                val carrier = state.carrierName
                if (carrier != null) {
                    CapabilityStatus(kind = GadgetStatusKind.Success, message = carrier)
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.cell_cap_carrier_unknown),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.cell_cap_network_type_name),
            detail = stringResource(R.string.cell_cap_network_type_detail),
            status = {
                CapabilityStatus(
                    kind = if (state.networkType == CellNetworkType.Unknown) {
                        GadgetStatusKind.Warning
                    } else {
                        GadgetStatusKind.Success
                    },
                    message = networkTypeLabel(state.networkType),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.cell_cap_signal_name),
            detail = stringResource(R.string.cell_cap_signal_detail),
            status = {
                if (!permissionGranted) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.cell_cap_no_permission),
                        action = CapabilityAction.RequestPermissions(
                            listOf(Manifest.permission.READ_PHONE_STATE),
                        ),
                    )
                } else {
                    CapabilityStatus(
                        kind = signalStatusKind(state.signalLevel),
                        message = stringResource(R.string.cell_cap_signal_bars, state.signalLevel),
                    )
                }
            },
        ),
        rootedCapability(
            nameRes = R.string.cell_cap_raw_modem_name,
            detailRes = R.string.cell_cap_raw_modem_detail,
            isRootedFlavor = isRootedFlavor,
        ),
        rootedCapability(
            nameRes = R.string.cell_cap_signal_deep_name,
            detailRes = R.string.cell_cap_signal_deep_detail,
            isRootedFlavor = isRootedFlavor,
        ),
    ),
)

@Composable
private fun rootedCapability(
    nameRes: Int,
    detailRes: Int,
    isRootedFlavor: Boolean,
): ModuleCapability = ModuleCapability(
    name = stringResource(nameRes),
    detail = stringResource(detailRes),
    status = {
        if (isRootedFlavor) {
            CapabilityStatus(
                kind = GadgetStatusKind.Success,
                message = stringResource(R.string.cell_cap_rooted_active),
            )
        } else {
            CapabilityStatus(
                kind = GadgetStatusKind.Warning,
                message = stringResource(R.string.cell_cap_rooted_required),
            )
        }
    },
)

@Composable
private fun simStateCapabilityStatus(simState: SimStateUi): CapabilityStatus = CapabilityStatus(
    kind = when (simState) {
        SimStateUi.Ready -> GadgetStatusKind.Success
        SimStateUi.Absent -> GadgetStatusKind.Error
        SimStateUi.Locked, SimStateUi.NetworkLocked, SimStateUi.NotReady, SimStateUi.Unknown ->
            GadgetStatusKind.Warning
    },
    message = simStateLabel(simState),
)

@Composable
private fun simStateLabel(simState: SimStateUi): String = stringResource(
    when (simState) {
        SimStateUi.Ready -> R.string.cell_sim_state_ready
        SimStateUi.Absent -> R.string.cell_sim_state_absent
        SimStateUi.Locked -> R.string.cell_sim_state_locked
        SimStateUi.NetworkLocked -> R.string.cell_sim_state_network_locked
        SimStateUi.NotReady -> R.string.cell_sim_state_not_ready
        SimStateUi.Unknown -> R.string.cell_sim_state_unknown
    },
)

@Composable
private fun networkTypeLabel(type: CellNetworkType): String = stringResource(
    when (type) {
        CellNetworkType.Nr5GPlus -> R.string.cell_network_type_5g_plus
        CellNetworkType.Nr5G -> R.string.cell_network_type_5g
        CellNetworkType.Lte4G -> R.string.cell_network_type_lte
        CellNetworkType.Umts3G -> R.string.cell_network_type_3g
        CellNetworkType.Gsm2G -> R.string.cell_network_type_2g
        CellNetworkType.Unknown -> R.string.cell_network_type_unknown
    },
)

private fun signalStatusKind(level: Int): GadgetStatusKind = when {
    level >= 3 -> GadgetStatusKind.Success
    level >= 1 -> GadgetStatusKind.Warning
    else -> GadgetStatusKind.Error
}

@Composable
internal fun CellScreenContent(
    state: CellState,
    isRootedFlavor: Boolean,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    rawModemDump: CellDumpUiState = CellDumpUiState.Idle,
    signalDeepDump: CellDumpUiState = CellDumpUiState.Idle,
    onRequestPermission: () -> Unit = {},
    onLoadRawModemDump: () -> Unit = {},
    onLoadSignalDeepDump: () -> Unit = {},
    rootTools: @Composable () -> Unit = {},
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.cell_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            if (!state.permissionGranted) {
                CellPermissionCard(onRequestPermission = onRequestPermission)
            } else {
                CellStatusCard(state = state)
                liveMonitors()
                monitors()
            }
            // Rooted-only "raw modem diagnostics" surface. Shown regardless
            // of READ_PHONE_STATE grant — it's a separate, root-gated data
            // path (RootSafetyGate), not a TelephonyManager reading.
            if (isRootedFlavor) {
                CellRootedDiagnosticsCard(
                    rawModemDump = rawModemDump,
                    signalDeepDump = signalDeepDump,
                    onLoadRawModemDump = onLoadRawModemDump,
                    onLoadSignalDeepDump = onLoadSignalDeepDump,
                )
            }
            rootTools()
        },
    )
}

@Composable
private fun CellPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.cell_permission_card_title),
    ) {
        Text(
            text = stringResource(R.string.cell_permission_card_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        GadgetPrimaryButton(
            onClick = onRequestPermission,
            text = stringResource(R.string.cell_permission_grant_button),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CellStatusCard(
    state: CellState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.cell_card_status_title),
        icon = Icons.Filled.SignalCellularAlt,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            CellDetailRow(
                label = stringResource(R.string.cell_label_sim_state),
                value = simStateLabel(state.simState),
            )
            CellDetailRow(
                label = stringResource(R.string.cell_label_carrier),
                value = state.carrierName ?: stringResource(R.string.cell_cap_carrier_unknown),
            )
            CellDetailRow(
                label = stringResource(R.string.cell_label_network_type),
                value = networkTypeLabel(state.networkType),
            )
            CellDetailRow(
                label = stringResource(R.string.cell_label_signal),
                value = stringResource(R.string.cell_cap_signal_bars, state.signalLevel),
            )
        }
    }
}

@Composable
private fun CellDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Rooted-only capability-row surface (per the migration brief: "the rooted
 * Qualcomm dump stays a capability-row surface"), expanded here into two
 * on-demand [GadgetExpandableCard] panels — one per read-only
 * [dev.ranzlappen.gadget.feature.radios.cell.control.CellController] method.
 * Each fetch is user-triggered (not run on screen entry) since both walk a
 * root shell over several sysfs globs.
 */
@Composable
private fun CellRootedDiagnosticsCard(
    rawModemDump: CellDumpUiState,
    signalDeepDump: CellDumpUiState,
    onLoadRawModemDump: () -> Unit,
    onLoadSignalDeepDump: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        var modemExpanded by rememberSaveable { mutableStateOf(false) }
        GadgetExpandableCard(
            title = stringResource(R.string.cell_root_modem_dump_title),
            expanded = modemExpanded,
            onExpandedChange = { modemExpanded = it },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Memory,
        ) {
            CellDumpBody(dumpState = rawModemDump, onLoad = onLoadRawModemDump)
        }
        var signalExpanded by rememberSaveable { mutableStateOf(false) }
        GadgetExpandableCard(
            title = stringResource(R.string.cell_root_signal_deep_title),
            expanded = signalExpanded,
            onExpandedChange = { signalExpanded = it },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.SignalCellularAlt,
        ) {
            CellDumpBody(dumpState = signalDeepDump, onLoad = onLoadSignalDeepDump)
        }
    }
}

@Composable
private fun CellDumpBody(
    dumpState: CellDumpUiState,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        when (dumpState) {
            CellDumpUiState.Idle -> {
                Text(
                    text = stringResource(R.string.cell_dump_idle),
                    style = MaterialTheme.typography.bodySmall,
                )
                GadgetSecondaryButton(
                    onClick = onLoad,
                    text = stringResource(R.string.cell_dump_fetch_button),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CellDumpUiState.Loading -> {
                GadgetSecondaryButton(
                    onClick = onLoad,
                    text = stringResource(R.string.cell_dump_fetch_button),
                    enabled = false,
                    loading = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is CellDumpUiState.Loaded -> {
                if (dumpState.nodes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cell_dump_empty),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        dumpState.nodes.forEach { (key, value) ->
                            CellDumpRow(key = key, value = value)
                        }
                    }
                }
                GadgetSecondaryButton(
                    onClick = onLoad,
                    text = stringResource(R.string.cell_dump_refresh_button),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CellDumpUiState.Unsupported -> {
                Text(
                    text = stringResource(R.string.cell_dump_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is CellDumpUiState.Error -> {
                Text(
                    text = dumpState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                GadgetSecondaryButton(
                    onClick = onLoad,
                    text = stringResource(R.string.cell_dump_retry_button),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CellDumpRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun CellScreenGrantedPreview() = GadgetThemedPreview {
    CellScreenContent(
        state = CellState(
            permissionGranted = true,
            simState = SimStateUi.Ready,
            carrierName = "Example Mobile",
            networkType = CellNetworkType.Lte4G,
            signalLevel = 3,
        ),
        isRootedFlavor = false,
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun CellScreenNoPermissionPreview() = GadgetThemedPreview {
    CellScreenContent(
        state = CellState(permissionGranted = false),
        isRootedFlavor = false,
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun CellScreenRootedDumpLoadedPreview() = GadgetThemedPreview {
    CellScreenContent(
        state = CellState(
            permissionGranted = true,
            simState = SimStateUi.Ready,
            carrierName = "Example Mobile",
            networkType = CellNetworkType.Nr5G,
            signalLevel = 4,
        ),
        isRootedFlavor = true,
        moduleInfo = null,
        rawModemDump = CellDumpUiState.Loaded(
            mapOf(
                "/sys/class/qcom_smd8/status" to "online",
                "/sys/class/net/rmnet0/mtu" to "1500",
            ),
        ),
        signalDeepDump = CellDumpUiState.Unsupported,
    )
}
