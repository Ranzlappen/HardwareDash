package dev.ranzlappen.gadget.feature.radios.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun WifiScreen(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    WifiScreenContent(
        state = state,
        isRootedFlavor = viewModel.isRootedFlavor,
        moduleInfo = wifiModuleInfo(state, viewModel.isRootedFlavor),
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.wifi_root_tools_title),
                available = viewModel.isRootedFlavor,
                unavailableMessage = stringResource(R.string.wifi_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.wifi_root_injection_label),
                    description = stringResource(R.string.wifi_root_injection_detail),
                    runLabel = stringResource(R.string.wifi_root_run),
                    onRun = viewModel::onProbeInjection,
                    enabled = !rootTools.injectionProbe.running,
                    statusMessage = rootTools.injectionProbe.message,
                    statusKind = rootTools.injectionProbe.statusKind,
                )
            }
        },
        monitors = {
            LiveMonitorContainer(
                metricKey = WifiSignalMetricSource.METRIC_KEY,
                title = stringResource(R.string.wifi_live_monitor_signal),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "wifi_live_${WifiSignalMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = WifiSignalMetricSource.METRIC_KEY,
                title = stringResource(R.string.wifi_monitor_signal),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "wifi_monitor_${WifiSignalMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = WifiEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.wifi_monitor_enabled),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "wifi_monitor_${WifiEnabledMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun wifiModuleInfo(state: WifiState, isRootedFlavor: Boolean): ModuleInfo {
    return ModuleInfo(
        compatibility = OsCompatibility(minSdk = 1),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.wifi_cap_adapter_name),
                detail = stringResource(R.string.wifi_cap_adapter_detail),
                status = {
                    if (state.enabled) {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.wifi_cap_adapter_enabled),
                        )
                    } else {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.wifi_cap_adapter_disabled),
                        )
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.wifi_cap_rfkill_name),
                detail = stringResource(R.string.wifi_cap_rfkill_detail),
                status = {
                    if (isRootedFlavor) {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.wifi_cap_rooted_active),
                        )
                    } else {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.wifi_cap_rooted_required),
                        )
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.wifi_cap_tx_power_name),
                detail = stringResource(R.string.wifi_cap_tx_power_detail),
                status = {
                    if (isRootedFlavor) {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.wifi_cap_rooted_active),
                        )
                    } else {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.wifi_cap_rooted_required),
                        )
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.wifi_cap_channel_name),
                detail = stringResource(R.string.wifi_cap_channel_detail),
                status = {
                    if (isRootedFlavor) {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.wifi_cap_rooted_active),
                        )
                    } else {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.wifi_cap_rooted_required),
                        )
                    }
                },
            ),
        ),
    )
}

@Composable
internal fun WifiScreenContent(
    state: WifiState,
    isRootedFlavor: Boolean = false,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
    rootTools: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.wifi_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            WifiStatusCard(state = state)
            if (state.connected) {
                WifiNetworkCard(state = state)
            }
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun WifiStatusCard(
    state: WifiState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.wifi_card_status_title),
        icon = Icons.Filled.Wifi,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = state.enabled,
                    onClick = {},
                    label = stringResource(
                        if (state.enabled) R.string.wifi_chip_enabled else R.string.wifi_chip_disabled,
                    ),
                    enabled = false,
                )
                if (state.enabled) {
                    GadgetChip(
                        selected = state.connected,
                        onClick = {},
                        label = stringResource(
                            if (state.connected) R.string.wifi_chip_connected
                            else R.string.wifi_chip_disconnected,
                        ),
                        enabled = false,
                    )
                }
            }
            when {
                !state.enabled -> Text(
                    text = stringResource(R.string.wifi_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                !state.connected -> Text(
                    text = stringResource(R.string.wifi_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WifiNetworkCard(
    state: WifiState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.wifi_card_network_title),
        icon = Icons.Filled.Wifi,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            WifiDetailRow(
                label = stringResource(R.string.wifi_label_ssid),
                value = state.ssid ?: stringResource(R.string.wifi_ssid_unknown),
            )
            state.rssiDbm?.let { rssi ->
                val quality = when {
                    rssi >= -60 -> stringResource(R.string.wifi_signal_excellent)
                    rssi >= -70 -> stringResource(R.string.wifi_signal_good)
                    rssi >= -80 -> stringResource(R.string.wifi_signal_fair)
                    else -> stringResource(R.string.wifi_signal_weak)
                }
                WifiDetailRow(
                    label = stringResource(R.string.wifi_label_rssi),
                    value = "${stringResource(R.string.wifi_rssi_format, rssi)} · $quality",
                )
            }
            state.linkSpeedMbps?.let { speed ->
                WifiDetailRow(
                    label = stringResource(R.string.wifi_label_speed),
                    value = stringResource(R.string.wifi_speed_format, speed),
                )
            }
            state.frequencyMhz?.let { freq ->
                val band = when (freq) {
                    in 2401..2495 -> stringResource(R.string.wifi_band_2_4ghz)
                    in 5150..5875 -> stringResource(R.string.wifi_band_5ghz)
                    in 5925..7125 -> stringResource(R.string.wifi_band_6ghz)
                    else -> null
                }
                WifiDetailRow(
                    label = stringResource(R.string.wifi_label_frequency),
                    value = "${stringResource(R.string.wifi_freq_format, freq)}${band?.let { " · $it" } ?: ""}",
                )
            }
            state.bssid?.let { bssid ->
                WifiDetailRow(
                    label = stringResource(R.string.wifi_label_bssid),
                    value = bssid,
                )
            }
        }
    }
}

@Composable
private fun WifiDetailRow(
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

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun WifiScreenConnectedPreview() = GadgetThemedPreview {
    WifiScreenContent(
        state = WifiState(
            enabled = true,
            connected = true,
            ssid = "HomeNetwork",
            rssiDbm = -55,
            linkSpeedMbps = 144,
            frequencyMhz = 5180,
            bssid = "AA:BB:CC:DD:EE:FF",
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun WifiScreenDisabledPreview() = GadgetThemedPreview {
    WifiScreenContent(
        state = WifiState(enabled = false),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun WifiScreenDisconnectedPreview() = GadgetThemedPreview {
    WifiScreenContent(
        state = WifiState(enabled = true, connected = false),
        moduleInfo = null,
    )
}
