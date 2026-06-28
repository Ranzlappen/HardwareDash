package dev.ranzlappen.gadget.feature.radios.subghz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun SubghzScreen(
    modifier: Modifier = Modifier,
    viewModel: SubghzViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SubghzScreenContent(
        state = state,
        moduleInfo = subghzModuleInfo(state, viewModel.isRootedFlavor),
        modifier = modifier,
        monitors = {
            LiveMonitorContainer(
                metricKey = SubghzConnectedMetricSource.METRIC_KEY,
                title = stringResource(R.string.subghz_live_monitor_bridge),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "subghz_live_${SubghzConnectedMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = SubghzConnectedMetricSource.METRIC_KEY,
                title = stringResource(R.string.subghz_monitor_bridge),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "subghz_monitor_${SubghzConnectedMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun subghzModuleInfo(state: SubghzState, isRootedFlavor: Boolean): ModuleInfo {
    return ModuleInfo(
        compatibility = OsCompatibility(minSdk = 1),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.subghz_cap_bridge_name),
                detail = stringResource(R.string.subghz_cap_bridge_detail),
                status = {
                    when {
                        state.bridgeConnected -> CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.subghz_cap_bridge_connected),
                        )
                        !state.usbHostAvailable -> CapabilityStatus(
                            kind = GadgetStatusKind.Error,
                            message = stringResource(R.string.subghz_cap_bridge_no_host),
                        )
                        else -> CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.subghz_cap_bridge_none),
                        )
                    }
                },
            ),
            rootedCapability(
                R.string.subghz_cap_registers_name,
                R.string.subghz_cap_registers_detail,
                isRootedFlavor,
            ),
            rootedCapability(
                R.string.subghz_cap_tuning_name,
                R.string.subghz_cap_tuning_detail,
                isRootedFlavor,
            ),
            rootedCapability(
                R.string.subghz_cap_capture_name,
                R.string.subghz_cap_capture_detail,
                isRootedFlavor,
            ),
        ),
    )
}

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
                message = stringResource(R.string.subghz_cap_rooted_active),
            )
        } else {
            CapabilityStatus(
                kind = GadgetStatusKind.Warning,
                message = stringResource(R.string.subghz_cap_rooted_required),
            )
        }
    },
)

@Composable
internal fun SubghzScreenContent(
    state: SubghzState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.subghz_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            SubghzBridgeCard(state = state)
            monitors()
        },
    )
}

@Composable
private fun SubghzBridgeCard(
    state: SubghzState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.subghz_card_bridge_title),
        icon = Icons.Filled.SettingsInputAntenna,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = state.bridgeConnected,
                    onClick = {},
                    label = stringResource(
                        if (state.bridgeConnected) R.string.subghz_chip_connected
                        else R.string.subghz_chip_none,
                    ),
                    enabled = false,
                )
                if (state.device?.coversSubGhz == true) {
                    GadgetChip(
                        selected = true,
                        onClick = {},
                        label = stringResource(R.string.subghz_chip_capable),
                        enabled = false,
                    )
                }
            }
            when {
                state.device != null -> {
                    SubghzDetailRow(
                        label = stringResource(R.string.subghz_label_device),
                        value = state.device.displayName,
                    )
                    SubghzDetailRow(
                        label = stringResource(R.string.subghz_label_coverage),
                        value = stringResource(
                            if (state.device.coversSubGhz) R.string.subghz_coverage_subghz
                            else R.string.subghz_coverage_wideband,
                        ),
                    )
                }
                !state.usbHostAvailable -> Text(
                    text = stringResource(R.string.subghz_no_usb_host),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    text = stringResource(R.string.subghz_no_bridge),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubghzDetailRow(
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
private fun SubghzScreenConnectedPreview() = GadgetThemedPreview {
    SubghzScreenContent(
        state = SubghzState(
            usbHostAvailable = true,
            device = SdrDevice.YardStickOne,
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun SubghzScreenNoBridgePreview() = GadgetThemedPreview {
    SubghzScreenContent(
        state = SubghzState(usbHostAvailable = true, device = null),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun SubghzScreenNoHostPreview() = GadgetThemedPreview {
    SubghzScreenContent(
        state = SubghzState(usbHostAvailable = false, device = null),
        moduleInfo = null,
    )
}
