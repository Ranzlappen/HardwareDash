package dev.ranzlappen.gadget.feature.flipper

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
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
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.flipper.monitor.FlipperBatteryMetricSource
import dev.ranzlappen.gadget.feature.flipper.monitor.FlipperConnectedMetricSource

@Composable
fun FlipperScreen(
    modifier: Modifier = Modifier,
    viewModel: FlipperViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshBondedDevices() }

    FlipperScreenContent(
        state = state,
        moduleInfo = flipperModuleInfo(state.isRootedFlavor),
        modifier = modifier,
        onConnectUsb = viewModel::connectUsb,
        onRequestBle = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blePermissionLauncher.launch(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                )
            } else {
                viewModel.refreshBondedDevices()
            }
        },
        onConnectBle = viewModel::connectBle,
        onDisconnect = viewModel::disconnect,
        onPing = viewModel::ping,
        monitors = {
            LiveMonitorContainer(
                metricKey = FlipperBatteryMetricSource.METRIC_KEY,
                title = stringResource(R.string.flipper_live_monitor_battery),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "flipper_live_${FlipperBatteryMetricSource.METRIC_KEY}",
            )
            MonitorContainer(
                metricKey = FlipperConnectedMetricSource.METRIC_KEY,
                title = stringResource(R.string.flipper_monitor_connected),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "flipper_monitor_${FlipperConnectedMetricSource.METRIC_KEY}",
            )
        },
    )
}

@Composable
private fun flipperModuleInfo(isRootedFlavor: Boolean): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.flipper_cap_usb_name),
            detail = stringResource(R.string.flipper_cap_usb_detail),
            status = {
                CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.flipper_cap_supported),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.flipper_cap_ble_name),
            detail = stringResource(R.string.flipper_cap_ble_detail),
            status = {
                CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.flipper_cap_supported),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.flipper_cap_usb_grant_name),
            detail = stringResource(R.string.flipper_cap_usb_grant_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.flipper_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.flipper_cap_rooted_required),
                    )
                }
            },
        ),
    ),
)

@Composable
internal fun FlipperScreenContent(
    state: FlipperUiState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    onConnectUsb: () -> Unit = {},
    onRequestBle: () -> Unit = {},
    onConnectBle: (String) -> Unit = {},
    onDisconnect: () -> Unit = {},
    onPing: () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.flipper_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            FlipperConnectionCard(
                connection = state.connection,
                onConnectUsb = onConnectUsb,
                onRequestBle = onRequestBle,
                onDisconnect = onDisconnect,
                onPing = onPing,
            )
            if (state.bleDevices.isNotEmpty() &&
                state.connection !is FlipperConnectionManager.State.Connected
            ) {
                FlipperBlePickerCard(devices = state.bleDevices, onConnectBle = onConnectBle)
            }
            monitors()
        },
    )
}

@Composable
private fun FlipperConnectionCard(
    connection: FlipperConnectionManager.State,
    onConnectUsb: () -> Unit,
    onRequestBle: () -> Unit,
    onDisconnect: () -> Unit,
    onPing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val connecting = connection is FlipperConnectionManager.State.Connecting
    val connected = connection is FlipperConnectionManager.State.Connected
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.flipper_card_connection_title),
        icon = Icons.Filled.Memory,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            when (connection) {
                is FlipperConnectionManager.State.Disconnected -> Text(
                    text = stringResource(R.string.flipper_disconnected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is FlipperConnectionManager.State.Connecting -> Text(
                    text = stringResource(R.string.flipper_connecting, connection.transport),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is FlipperConnectionManager.State.Connected -> {
                    FlipperDetailRow(
                        stringResource(R.string.flipper_label_transport),
                        connection.transport,
                    )
                    FlipperDetailRow(
                        stringResource(R.string.flipper_label_device),
                        connection.deviceName ?: stringResource(R.string.flipper_unknown),
                    )
                    FlipperDetailRow(
                        stringResource(R.string.flipper_label_firmware),
                        connection.firmwareVersion ?: stringResource(R.string.flipper_unknown),
                    )
                    connection.batteryPercent?.let {
                        FlipperDetailRow(
                            stringResource(R.string.flipper_label_battery),
                            stringResource(R.string.flipper_battery_format, it),
                        )
                    }
                }
                is FlipperConnectionManager.State.Failed -> Text(
                    text = stringResource(R.string.flipper_failed, connection.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (connected) {
                    GadgetSecondaryButton(
                        onClick = onPing,
                        text = stringResource(R.string.flipper_action_ping),
                    )
                    GadgetSecondaryButton(
                        onClick = onDisconnect,
                        text = stringResource(R.string.flipper_action_disconnect),
                    )
                } else {
                    GadgetPrimaryButton(
                        onClick = onConnectUsb,
                        text = stringResource(R.string.flipper_action_connect_usb),
                        enabled = !connecting,
                        loading = connecting,
                    )
                    GadgetSecondaryButton(
                        onClick = onRequestBle,
                        text = stringResource(R.string.flipper_action_connect_ble),
                        enabled = !connecting,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipperBlePickerCard(
    devices: List<BleDeviceUi>,
    onConnectBle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.flipper_card_ble_title),
        icon = Icons.Filled.Memory,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            devices.forEach { device ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    GadgetSecondaryButton(
                        onClick = { onConnectBle(device.address) },
                        text = stringResource(R.string.flipper_action_connect),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipperDetailRow(
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
private fun FlipperScreenConnectedPreview() = GadgetThemedPreview {
    FlipperScreenContent(
        state = FlipperUiState(
            connection = FlipperConnectionManager.State.Connected(
                transport = "USB",
                deviceName = "Flipper Wabbit",
                firmwareVersion = "0.103.1",
                batteryPercent = 87,
            ),
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun FlipperScreenDisconnectedPreview() = GadgetThemedPreview {
    FlipperScreenContent(
        state = FlipperUiState(
            connection = FlipperConnectionManager.State.Disconnected,
            bleDevices = listOf(BleDeviceUi("Flipper Wabbit", "AA:BB:CC:DD:EE:FF")),
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun FlipperScreenFailedPreview() = GadgetThemedPreview {
    FlipperScreenContent(
        state = FlipperUiState(
            connection = FlipperConnectionManager.State.Failed("No Flipper Zero attached via USB"),
        ),
        moduleInfo = null,
    )
}
