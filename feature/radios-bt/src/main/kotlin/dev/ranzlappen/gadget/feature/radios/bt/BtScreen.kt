package dev.ranzlappen.gadget.feature.radios.bt

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Hilt entry point for the Bluetooth feature screen. Collects state from
 * [BtViewModel], refreshes on every ON_RESUME (permission changes take
 * effect when the user returns from Settings), and delegates rendering
 * to the stateless [BtScreenContent] — keeping it preview-friendly.
 */
@Composable
fun BtScreen(
    modifier: Modifier = Modifier,
    viewModel: BtViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BtScreenContent(
        state = state,
        moduleInfo = btModuleInfo(state),
        modifier = modifier,
        monitors = {
            MonitorContainer(
                metricKey = BtEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.bt_monitor_title),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "bt_monitor_${BtEnabledMetricSource.METRIC_KEY}",
            )
        },
    )
}

/**
 * Builds the [ModuleInfo] for the Bluetooth screen — a single capability row
 * that reflects adapter presence and enabled state.
 */
@Composable
private fun btModuleInfo(state: BtState): ModuleInfo {
    val ctx = LocalContext.current
    return ModuleInfo(
        compatibility = OsCompatibility(minSdk = 5),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.bt_capability_adapter),
                detail = stringResource(R.string.bt_capability_adapter_detail),
                status = {
                    when {
                        !state.adapterAvailable -> CapabilityStatus(
                            kind = GadgetStatusKind.Error,
                            message = stringResource(R.string.bt_adapter_disabled),
                        )
                        !state.permissionGranted -> CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.bt_permission_required),
                            action = CapabilityAction.Custom(
                                label = stringResource(R.string.bt_permission_grant),
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${ctx.packageName}"),
                                    )
                                    ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                            ),
                        )
                        state.adapterEnabled -> CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.bt_adapter_enabled),
                        )
                        else -> CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.bt_adapter_disabled),
                        )
                    }
                },
            ),
        ),
    )
}

/**
 * Stateless Bluetooth screen content — renders the capability card, status
 * card, bonded device list, and the monitor container slot.
 */
@Composable
internal fun BtScreenContent(
    state: BtState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.bt_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            BtStatusCard(state = state)
            monitors()
        },
    )
}

/**
 * Card showing adapter name, on/off status, and the bonded device list.
 * Shows a permission prompt when BLUETOOTH_CONNECT is not granted on
 * Android 12+.
 */
@Composable
private fun BtStatusCard(
    state: BtState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val ctx = LocalContext.current

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.bt_capability_card_title),
        icon = Icons.Filled.Bluetooth,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            // Adapter presence + enabled status chips
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = state.adapterAvailable,
                    onClick = {},
                    label = stringResource(R.string.bt_adapter_present),
                    enabled = false,
                )
                GadgetChip(
                    selected = state.adapterEnabled,
                    onClick = {},
                    label = if (state.adapterEnabled) {
                        stringResource(R.string.bt_adapter_enabled)
                    } else {
                        stringResource(R.string.bt_adapter_disabled)
                    },
                    enabled = false,
                )
            }

            if (!state.permissionGranted) {
                Spacer(modifier = Modifier.height(spacing.tiny))
                Text(
                    text = stringResource(R.string.bt_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${ctx.packageName}"),
                        )
                        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                ) {
                    Text(stringResource(R.string.bt_permission_grant))
                }
            } else {
                // Adapter name
                state.adapterName?.let { name ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        Text(
                            text = stringResource(R.string.bt_adapter_name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = spacing.tiny),
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Bonded devices
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = stringResource(R.string.bt_bonded_devices_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.bondedDevices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bt_no_bonded_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.bondedDevices.forEach { device ->
                        BondedDeviceRow(device = device)
                    }
                }
            }
        }
    }
}

@Composable
private fun BondedDeviceRow(
    device: BluetoothDeviceInfo,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.pico),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name ?: device.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (device.name != null) {
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GadgetChip(
            selected = false,
            onClick = {},
            label = device.typeName,
            enabled = false,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun BtScreenPreview() = GadgetThemedPreview {
    BtScreenContent(
        state = BtState(
            adapterAvailable = true,
            adapterEnabled = true,
            adapterName = "Pixel 8",
            permissionGranted = true,
            bondedDevices = listOf(
                BluetoothDeviceInfo(name = "Galaxy Buds2", address = "AA:BB:CC:DD:EE:FF", typeName = "Classic"),
                BluetoothDeviceInfo(name = null, address = "11:22:33:44:55:66", typeName = "BLE"),
            ),
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun BtScreenNoPermissionPreview() = GadgetThemedPreview {
    BtScreenContent(
        state = BtState(
            adapterAvailable = true,
            adapterEnabled = false,
            adapterName = null,
            permissionGranted = false,
            bondedDevices = emptyList(),
        ),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun BtScreenNoAdapterPreview() = GadgetThemedPreview {
    BtScreenContent(
        state = BtState(
            adapterAvailable = false,
            adapterEnabled = false,
            adapterName = null,
            permissionGranted = true,
            bondedDevices = emptyList(),
        ),
        moduleInfo = null,
    )
}
