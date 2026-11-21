package dev.ranzlappen.gadget.feature.radios.nfc

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
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
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusDot
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.color
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
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NdefRecordKind
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NdefRecordView
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagInfo
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteMode
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteOutcome
import dev.ranzlappen.gadget.feature.radios.nfc.tag.TagProtection
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
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // Reader mode while the screen is in front (v3 B11): tags come straight
    // to the view model instead of the system tag dispatcher. Reader mode
    // switches card emulation off, so it pauses while HCE is active.
    val readerOn = state.adapterEnabled && state.hceMode == NfcHceMode.NONE
    DisposableEffect(lifecycleOwner, activity, readerOn) {
        val adapter = activity?.let { NfcAdapter.getDefaultAdapter(it) }
        fun enableReader() {
            if (readerOn && activity != null) {
                runCatching { adapter?.enableReaderMode(activity, viewModel::onTag, READER_FLAGS, null) }
            }
        }
        fun disableReader() {
            if (activity != null) runCatching { adapter?.disableReaderMode(activity) }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refresh()
                    enableReader()
                }
                Lifecycle.Event.ON_PAUSE -> disableReader()
                else -> Unit
            }
        }
        // Replays ON_RESUME immediately when the screen is already resumed.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disableReader()
        }
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
                is NfcUiEvent.SetWriteMode -> viewModel.setWriteMode(event.mode)
                is NfcUiEvent.SetWriteText -> viewModel.setWriteText(event.text)
                NfcUiEvent.ArmWrite -> viewModel.armWrite()
                NfcUiEvent.CancelWrite -> viewModel.cancelWrite()
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
            if (state.isRootedFlavor) {
                NfcToolsCard(
                    enabled = true,
                    sendNci = rootTools.sendNci,
                    onSendRawNci = viewModel::onSendRawNci,
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
    data class SetWriteMode(val mode: NfcWriteMode) : NfcUiEvent
    data class SetWriteText(val text: String) : NfcUiEvent
    data object ArmWrite : NfcUiEvent
    data object CancelWrite : NfcUiEvent
}

private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
            NfcWriteCard(state = state, onEvent = onEvent)
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
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_tag_card_title),
        icon = Icons.Outlined.Nfc,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            val hint = when {
                state.readingTag -> stringResource(R.string.nfc_tag_reading)
                state.hceMode != NfcHceMode.NONE -> stringResource(R.string.nfc_tag_reading_paused)
                state.tag == null -> stringResource(R.string.nfc_tag_empty)
                else -> null
            }
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.tag?.let { TagDetails(it) }
        }
    }
}

@Composable
private fun TagDetails(tag: NfcTagInfo) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        TagField(stringResource(R.string.nfc_tag_id), tag.uid)
        tag.chip?.let { TagField(stringResource(R.string.nfc_tag_chip), it) }
        TagField(stringResource(R.string.nfc_tag_techs), tag.techs.joinToString(", "))
        TagField(
            stringResource(R.string.nfc_tag_ndef),
            if (tag.ndefType == null) {
                stringResource(R.string.nfc_tag_not_ndef)
            } else {
                stringResource(
                    R.string.nfc_tag_ndef_usage,
                    tag.ndefType,
                    tag.ndefUsedBytes ?: 0,
                    tag.ndefMaxBytes ?: 0,
                ) + " · " + stringResource(
                    if (tag.writable == true) R.string.nfc_tag_writable else R.string.nfc_tag_read_only,
                )
            },
        )
    }
    ProtectionSection(tag.protection)
    Text(
        text = stringResource(R.string.nfc_tag_records, tag.records.size),
        style = MaterialTheme.typography.titleSmall,
    )
    if (tag.records.isEmpty()) {
        Text(
            text = stringResource(R.string.nfc_tag_no_records),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    tag.records.forEach { RecordRow(it) }
}

@Composable
private fun TagField(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProtectionSection(protection: TagProtection?) {
    val spacing = LocalGadgetTheme.current.spacing
    Text(
        text = stringResource(R.string.nfc_protection_title),
        style = MaterialTheme.typography.titleSmall,
    )
    if (protection == null) {
        ProtectionLine(GadgetStatusKind.Warning, stringResource(R.string.nfc_protection_unknown))
        return
    }
    if (!protection.anyProtection) {
        ProtectionLine(GadgetStatusKind.Success, stringResource(R.string.nfc_protection_none))
    }
    if (protection.lockBitsSet == true) {
        ProtectionLine(GadgetStatusKind.Error, stringResource(R.string.nfc_protection_locked))
    }
    if (protection.readOnlyFlag == true) {
        ProtectionLine(GadgetStatusKind.Warning, stringResource(R.string.nfc_protection_read_only))
    }
    if (protection.passwordActive == true) {
        ProtectionLine(GadgetStatusKind.Warning, stringResource(R.string.nfc_protection_password))
    }
    if (protection.details.isNotEmpty()) {
        Text(
            text = protection.details.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.medium),
        )
    }
}

@Composable
private fun ProtectionLine(kind: GadgetStatusKind, text: String) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        GadgetStatusDot(contentDescription = null, color = kind.color())
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecordRow(record: NdefRecordView) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(record.kind.labelRes()) + (record.detail?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = record.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (record.value.isNotEmpty()) {
            GadgetIconButton(
                onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("NFC record", record.value))
                },
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.nfc_record_copy),
            )
        }
    }
}

private fun NdefRecordKind.labelRes(): Int = when (this) {
    NdefRecordKind.Text -> R.string.nfc_record_text
    NdefRecordKind.Uri -> R.string.nfc_record_uri
    NdefRecordKind.Mime -> R.string.nfc_record_mime
    NdefRecordKind.External -> R.string.nfc_record_external
    NdefRecordKind.SmartPoster -> R.string.nfc_record_smart_poster
    NdefRecordKind.Empty -> R.string.nfc_record_empty
    NdefRecordKind.Other -> R.string.nfc_record_other
}

@Composable
private fun NfcWriteCard(
    state: NfcState,
    onEvent: (NfcUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_write_card_title),
        icon = Icons.Outlined.Edit,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                GadgetChip(
                    selected = state.writeMode == NfcWriteMode.Text,
                    onClick = { onEvent(NfcUiEvent.SetWriteMode(NfcWriteMode.Text)) },
                    label = stringResource(R.string.nfc_write_mode_text),
                    enabled = !state.writeArmed,
                )
                GadgetChip(
                    selected = state.writeMode == NfcWriteMode.Url,
                    onClick = { onEvent(NfcUiEvent.SetWriteMode(NfcWriteMode.Url)) },
                    label = stringResource(R.string.nfc_write_mode_url),
                    enabled = !state.writeArmed,
                )
            }
            GadgetTextField(
                value = state.writeText,
                onValueChange = { onEvent(NfcUiEvent.SetWriteText(it)) },
                label = stringResource(
                    if (state.writeMode == NfcWriteMode.Url) R.string.nfc_write_url_label else R.string.nfc_write_text_label,
                ),
                enabled = !state.writeArmed,
                singleLine = state.writeMode == NfcWriteMode.Url,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.writeArmed) {
                Text(
                    text = stringResource(R.string.nfc_write_armed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                GadgetSecondaryButton(
                    onClick = { onEvent(NfcUiEvent.CancelWrite) },
                    text = stringResource(R.string.nfc_write_cancel),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                GadgetPrimaryButton(
                    onClick = { onEvent(NfcUiEvent.ArmWrite) },
                    text = stringResource(R.string.nfc_write_arm),
                    enabled = state.canWrite && state.adapterEnabled && state.hceMode == NfcHceMode.NONE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.writeOutcome?.let { outcome ->
                val (kind, text) = writeOutcomeText(outcome)
                ProtectionLine(kind, text)
            }
        }
    }
}

@Composable
private fun writeOutcomeText(outcome: NfcWriteOutcome): Pair<GadgetStatusKind, String> = when (outcome) {
    is NfcWriteOutcome.Written -> GadgetStatusKind.Success to stringResource(
        if (outcome.formatted) R.string.nfc_write_done_formatted else R.string.nfc_write_done,
        outcome.bytes,
    )
    NfcWriteOutcome.ReadOnly -> GadgetStatusKind.Error to stringResource(R.string.nfc_write_read_only)
    is NfcWriteOutcome.TooLarge -> GadgetStatusKind.Error to
        stringResource(R.string.nfc_write_too_large, outcome.bytes, outcome.maxBytes)
    NfcWriteOutcome.Unsupported -> GadgetStatusKind.Error to stringResource(R.string.nfc_write_unsupported)
    NfcWriteOutcome.TagLost -> GadgetStatusKind.Warning to stringResource(R.string.nfc_write_tag_lost)
    is NfcWriteOutcome.Failed -> GadgetStatusKind.Error to stringResource(R.string.nfc_write_failed, outcome.message)
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

@GadgetPreviewLightDark
@Composable
private fun NfcScreenTagPreview() {
    GadgetThemedPreview {
        NfcScreenContent(
            state = NfcState(
                adapterPresent = true,
                adapterEnabled = true,
                tag = NfcTagInfo(
                    uid = "04:A2:3B:1C:55:80:00",
                    techs = listOf("NfcA", "MifareUltralight", "Ndef"),
                    chip = "NTAG215",
                    ndefType = "Type 2",
                    ndefMaxBytes = 496,
                    ndefUsedBytes = 23,
                    writable = true,
                    records = listOf(NdefRecordView(NdefRecordKind.Uri, "https://example.com")),
                    protection = TagProtection(false, false, false, listOf("Static lock 0x0000")),
                ),
                writeText = "example.com",
                writeMode = NfcWriteMode.Url,
                writeOutcome = NfcWriteOutcome.Written(23, formatted = false),
            ),
            onEvent = {},
        )
    }
}
