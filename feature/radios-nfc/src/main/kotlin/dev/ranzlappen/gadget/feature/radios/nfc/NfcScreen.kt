package dev.ranzlappen.gadget.feature.radios.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootConfirmActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate

@Composable
fun NfcScreen(
    modifier: Modifier = Modifier,
    viewModel: NfcViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val rootTools by viewModel.rootTools.collectAsState()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    NfcScreenContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is NfcUiEvent.SetHcePayload -> viewModel.setHcePayload(event.text)
                is NfcUiEvent.ActivateHce -> viewModel.activateHce(event.mode)
                NfcUiEvent.ClearHce -> viewModel.clearHce()
                NfcUiEvent.OpenTemplatePicker -> viewModel.openTemplatePicker()
                NfcUiEvent.CloseTemplatePicker -> viewModel.closeTemplatePicker()
                is NfcUiEvent.SelectTemplate -> viewModel.selectTemplate(event.template)
                is NfcUiEvent.SetTemplateValue -> viewModel.setTemplateValue(event.key, event.value)
                NfcUiEvent.ApplyTemplate -> viewModel.applyTemplate()
            }
        },
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.nfc_root_tools_title),
                available = state.isRootedFlavor,
                unavailableMessage = stringResource(R.string.nfc_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootConfirmActionRow(
                    label = stringResource(R.string.nfc_root_reset_label),
                    description = stringResource(R.string.nfc_root_reset_detail),
                    runLabel = stringResource(R.string.nfc_root_run),
                    confirmTitle = stringResource(R.string.nfc_root_reset_confirm_title),
                    confirmMessage = stringResource(R.string.nfc_root_reset_confirm_message),
                    confirmLabel = stringResource(R.string.nfc_root_reset_confirm_label),
                    cancelLabel = stringResource(R.string.nfc_root_cancel),
                    onConfirm = viewModel::onResetMutations,
                    enabled = !rootTools.reset.running,
                    statusMessage = rootTools.reset.message,
                    statusKind = rootTools.reset.statusKind,
                )
            }
        },
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = NfcEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.nfc_live_monitor_title),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "nfc_live_monitor",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = NfcEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.nfc_monitor_title),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "nfc_monitor",
            )
        },
    )
}

sealed interface NfcUiEvent {
    data class SetHcePayload(val text: String) : NfcUiEvent
    data class ActivateHce(val mode: NfcHceMode) : NfcUiEvent
    data object ClearHce : NfcUiEvent
    data object OpenTemplatePicker : NfcUiEvent
    data object CloseTemplatePicker : NfcUiEvent
    data class SelectTemplate(val template: NfcTemplate) : NfcUiEvent
    data class SetTemplateValue(val key: String, val value: String) : NfcUiEvent
    data object ApplyTemplate : NfcUiEvent
}

@Composable
private fun nfcModuleInfo(state: NfcState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 10),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.nfc_capability_adapter),
            detail = stringResource(R.string.nfc_capability_adapter_detail),
            status = {
                when {
                    !state.adapterPresent -> CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.nfc_adapter_present),
                    )
                    state.adapterEnabled -> CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.nfc_adapter_enabled),
                    )
                    else -> CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.nfc_adapter_disabled),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.nfc_cap_raw_nci_name),
            detail = stringResource(R.string.nfc_cap_raw_nci_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.nfc_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.nfc_cap_rooted_required),
                    )
                }
            },
        ),
    ),
)

@Composable
fun NfcScreenContent(
    state: NfcState,
    onEvent: (NfcUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    rootTools: @Composable () -> Unit = {},
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        moduleInfo = nfcModuleInfo(state),
        modifier = modifier,
        functional = {
            NfcCapabilityCard(state = state)
            NfcTagCard(state = state)
            NfcTemplateCard(state = state, onEvent = onEvent)
            NfcHceCard(state = state, onEvent = onEvent)
            liveMonitors()
            monitors()
            rootTools()
        },
    )
}

@Composable
private fun NfcCapabilityCard(state: NfcState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    val ctx = LocalContext.current
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_capability_card_title),
        icon = Icons.Outlined.Nfc,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                GadgetChip(
                    selected = state.adapterPresent,
                    onClick = {},
                    label = stringResource(R.string.nfc_adapter_present),
                )
            }
            if (state.adapterPresent && !state.adapterEnabled) {
                GadgetSecondaryButton(
                    onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_NFC_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    text = stringResource(R.string.nfc_adapter_disabled),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NfcTagCard(state: NfcState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    val ctx = LocalContext.current
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_tag_card_title),
    ) {
        if (state.lastTagPayload == null && state.lastTagId == null) {
            Text(
                text = stringResource(R.string.nfc_tag_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                state.lastTagId?.let { id ->
                    Text(
                        text = "${stringResource(R.string.nfc_tag_id)}: $id",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.lastTagFormat?.let { fmt ->
                    Text(
                        text = "${stringResource(R.string.nfc_tag_format)}: $fmt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.lastTagPayload?.let { payload ->
                    Text(
                        text = payload,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val clipboard = ctx.getSystemService(android.content.ClipboardManager::class.java)
                    GadgetSecondaryButton(
                        onClick = {
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("NFC payload", payload)
                            )
                        },
                        text = stringResource(R.string.nfc_tag_copy),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NfcTemplateCard(
    state: NfcState,
    onEvent: (NfcUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_template_card_title),
    ) {
        if (!state.showTemplatePicker) {
            GadgetSecondaryButton(
                onClick = { onEvent(NfcUiEvent.OpenTemplatePicker) },
                text = stringResource(R.string.nfc_template_browse),
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (state.selectedTemplate == null) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.nfc_template_pick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { onEvent(NfcUiEvent.CloseTemplatePicker) }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
                val categories = state.templates.map { it.category }.distinct()
                categories.forEach { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    state.templates.filter { it.category == category }.forEach { template ->
                        GadgetChip(
                            selected = false,
                            onClick = { onEvent(NfcUiEvent.SelectTemplate(template)) },
                            label = template.name,
                            modifier = Modifier.padding(vertical = spacing.pico),
                        )
                    }
                }
            }
        } else {
            val template = state.selectedTemplate
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = template.name, style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { onEvent(NfcUiEvent.CloseTemplatePicker) }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                }
                template.placeholders.forEach { key ->
                    OutlinedTextField(
                        value = state.templateValues[key] ?: "",
                        onValueChange = { onEvent(NfcUiEvent.SetTemplateValue(key, it)) },
                        label = { Text(key) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                GadgetPrimaryButton(
                    onClick = { onEvent(NfcUiEvent.ApplyTemplate) },
                    text = stringResource(R.string.nfc_template_activate),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NfcHceCard(
    state: NfcState,
    onEvent: (NfcUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_hce_card_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            GadgetChip(
                selected = state.hceMode != NfcHceMode.NONE,
                onClick = {},
                label = if (state.hceMode != NfcHceMode.NONE) {
                    stringResource(R.string.nfc_hce_active)
                } else {
                    stringResource(R.string.nfc_hce_inactive)
                },
            )
            OutlinedTextField(
                value = state.hcePayload,
                onValueChange = { onEvent(NfcUiEvent.SetHcePayload(it)) },
                label = { Text(stringResource(R.string.nfc_hce_text_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                GadgetPrimaryButton(
                    onClick = { onEvent(NfcUiEvent.ActivateHce(NfcHceMode.TEXT)) },
                    text = stringResource(R.string.nfc_hce_activate),
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = { onEvent(NfcUiEvent.ClearHce) },
                    text = stringResource(R.string.nfc_hce_clear),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun NfcScreenContentPreview() {
    GadgetThemedPreview {
        NfcScreenContent(
            state = NfcState(adapterPresent = true, adapterEnabled = true),
            onEvent = {},
        )
    }
}
