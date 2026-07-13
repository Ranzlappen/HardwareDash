@file:OptIn(ExperimentalLayoutApi::class)

package dev.ranzlappen.gadget.feature.adbdebug

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkPortRange
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbSetPropAllowList

@Composable
fun AdbDebugScreen(
    modifier: Modifier = Modifier,
    viewModel: AdbDebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    AdbDebugScreenContent(
        state = state,
        moduleInfo = adbDebugModuleInfo(state),
        onEvent = viewModel::onEvent,
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.adbdebug_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.adbdebug_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootActionRow(
                    label = stringResource(R.string.adbdebug_root_getprop_label),
                    description = stringResource(R.string.adbdebug_root_getprop_detail),
                    runLabel = stringResource(R.string.adbdebug_root_run),
                    onRun = viewModel::onDumpProperties,
                    enabled = !rootTools.properties.running,
                    statusMessage = rootTools.properties.message,
                    statusKind = rootTools.properties.statusKind,
                )
            }
        },
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = AdbEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.adbdebug_live_monitor_enabled),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "adbdebug_live_enabled",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = AdbEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.adbdebug_monitor_enabled),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "adbdebug_history_enabled",
            )
        },
    )
}

@Composable
private fun adbDebugModuleInfo(state: AdbDebugState): ModuleInfo {
    val context = LocalContext.current
    val openDevOptionsLabel = stringResource(R.string.adbdebug_action_open_dev_options)
    return ModuleInfo(
        compatibility = OsCompatibility(minSdk = 1),
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.adbdebug_cap_state_name),
                detail = stringResource(R.string.adbdebug_cap_state_detail),
                status = {
                    CapabilityStatus(
                        kind = if (state.adbEnabled) GadgetStatusKind.Success else GadgetStatusKind.Warning,
                        message = stringResource(
                            if (state.adbEnabled) R.string.adbdebug_cap_state_enabled
                            else R.string.adbdebug_cap_state_disabled,
                        ),
                        action = CapabilityAction.Custom(
                            label = openDevOptionsLabel,
                            onClick = { context.openDeveloperOptions() },
                        ),
                    )
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.adbdebug_cap_toggle_name),
                detail = stringResource(R.string.adbdebug_cap_toggle_detail),
                status = { rootedCapabilityStatus(state.isRootedFlavor) },
            ),
            ModuleCapability(
                name = stringResource(R.string.adbdebug_cap_network_name),
                detail = stringResource(R.string.adbdebug_cap_network_detail),
                status = { rootedCapabilityStatus(state.isRootedFlavor) },
            ),
            ModuleCapability(
                name = stringResource(R.string.adbdebug_cap_dump_name),
                detail = stringResource(R.string.adbdebug_cap_dump_detail),
                status = { rootedCapabilityStatus(state.isRootedFlavor) },
            ),
            ModuleCapability(
                name = stringResource(R.string.adbdebug_cap_setprop_name),
                detail = stringResource(R.string.adbdebug_cap_setprop_detail),
                status = { rootedCapabilityStatus(state.isRootedFlavor) },
            ),
        ),
    )
}

@Composable
private fun rootedCapabilityStatus(isRootedFlavor: Boolean): CapabilityStatus = if (isRootedFlavor) {
    CapabilityStatus(
        kind = GadgetStatusKind.Success,
        message = stringResource(R.string.adbdebug_cap_rooted_active),
    )
} else {
    CapabilityStatus(
        kind = GadgetStatusKind.Warning,
        message = stringResource(R.string.adbdebug_cap_rooted_required),
    )
}

private fun Context.openDeveloperOptions() {
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

@Composable
internal fun AdbDebugScreenContent(
    state: AdbDebugState,
    moduleInfo: ModuleInfo?,
    onEvent: (AdbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    rootTools: @Composable () -> Unit = {},
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.adbdebug_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            AdbStateCard(state = state)
            if (state.isRootedFlavor) {
                AdbToggleCard(state = state, onEvent = onEvent)
                AdbNetworkCard(state = state, onEvent = onEvent)
                AdbPropDumpCard(state = state, onEvent = onEvent)
                AdbSetPropCard(state = state, onEvent = onEvent)
            }
            liveMonitors()
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun AdbStateCard(state: AdbDebugState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.adbdebug_card_state_title),
        icon = Icons.Filled.DeveloperMode,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetChip(
                selected = state.adbEnabled,
                onClick = {},
                enabled = false,
                label = stringResource(
                    if (state.adbEnabled) R.string.adbdebug_chip_enabled else R.string.adbdebug_chip_disabled,
                ),
            )
            Text(
                text = stringResource(R.string.adbdebug_card_state_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdbToggleCard(
    state: AdbDebugState,
    onEvent: (AdbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.adbdebug_card_toggle_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.adbdebug_toggle_label),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.adbEnabled,
                    onCheckedChange = { onEvent(AdbDebugUiEvent.ToggleAdbEnabled(it)) },
                )
            }
            AdbLastResult(state)
        }
    }
}

@Composable
private fun AdbNetworkCard(
    state: AdbDebugState,
    onEvent: (AdbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.adbdebug_card_network_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(
                    R.string.adbdebug_network_range_hint,
                    AdbNetworkPortRange.MIN,
                    AdbNetworkPortRange.MAX,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetTextField(
                    value = state.networkPortText,
                    onValueChange = { onEvent(AdbDebugUiEvent.NetworkPortChange(it)) },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.adbdebug_network_port_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Switch(
                    checked = state.networkEnabled,
                    onCheckedChange = { onEvent(AdbDebugUiEvent.NetworkEnabledChange(it)) },
                )
            }
            GadgetSecondaryButton(
                onClick = { onEvent(AdbDebugUiEvent.ApplyNetworkSettings) },
                text = stringResource(R.string.adbdebug_network_apply),
                modifier = Modifier.fillMaxWidth(),
            )
            AdbLastResult(state)
        }
    }
}

@Composable
private fun AdbPropDumpCard(
    state: AdbDebugState,
    onEvent: (AdbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.adbdebug_card_dump_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.adbdebug_dump_persist_label),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.persistDumpToStorage,
                    onCheckedChange = { onEvent(AdbDebugUiEvent.PersistDumpChange(it)) },
                )
            }
            GadgetSecondaryButton(
                onClick = { onEvent(AdbDebugUiEvent.DumpProperties) },
                text = stringResource(R.string.adbdebug_dump_action),
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            state.lastDumpExcerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.lastDumpPersistedPath?.let { path ->
                Text(
                    text = stringResource(R.string.adbdebug_dump_saved_to, path),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AdbSetPropCard(
    state: AdbDebugState,
    onEvent: (AdbDebugUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.adbdebug_card_setprop_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            ) {
                AdbSetPropAllowList.EXACT_KEYS.forEach { key ->
                    GadgetChip(
                        selected = !state.setPropUsingLogTag && state.setPropKey == key,
                        onClick = { onEvent(AdbDebugUiEvent.SetPropKeyChange(key)) },
                        label = key,
                    )
                }
                GadgetChip(
                    selected = state.setPropUsingLogTag,
                    onClick = { onEvent(AdbDebugUiEvent.SetPropUseLogTag) },
                    label = stringResource(R.string.adbdebug_setprop_logtag_chip),
                )
            }
            if (state.setPropUsingLogTag) {
                GadgetTextField(
                    value = state.setPropLogTagSuffix,
                    onValueChange = { onEvent(AdbDebugUiEvent.SetPropLogTagSuffixChange(it)) },
                    label = stringResource(R.string.adbdebug_setprop_logtag_suffix_label),
                    placeholder = AdbSetPropAllowList.LOG_TAG_PREFIX + "MyTag",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GadgetTextField(
                value = state.setPropValue,
                onValueChange = { onEvent(AdbDebugUiEvent.SetPropValueChange(it)) },
                label = stringResource(R.string.adbdebug_setprop_value_label),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = { onEvent(AdbDebugUiEvent.ApplySetProp) },
                text = stringResource(R.string.adbdebug_setprop_apply),
                modifier = Modifier.fillMaxWidth(),
            )
            AdbLastResult(state)
        }
    }
}

@Composable
private fun AdbLastResult(state: AdbDebugState) {
    state.lastActionMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
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
private fun AdbDebugScreenStandardPreview() = GadgetThemedPreview {
    AdbDebugScreenContent(
        state = AdbDebugState(isRootedFlavor = false, adbEnabled = true),
        moduleInfo = null,
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun AdbDebugScreenRootedPreview() = GadgetThemedPreview {
    AdbDebugScreenContent(
        state = AdbDebugState(
            isRootedFlavor = true,
            adbEnabled = true,
            networkEnabled = true,
            networkPortText = "5555",
            lastDumpExcerpt = "[ro.build.version.sdk]: [34]\n[ro.product.model]: [Pixel]",
            lastActionMessage = "ADB enabled",
        ),
        moduleInfo = null,
        onEvent = {},
    )
}
