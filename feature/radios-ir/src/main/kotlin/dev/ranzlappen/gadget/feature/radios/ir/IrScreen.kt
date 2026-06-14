package dev.ranzlappen.gadget.feature.radios.ir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
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

@Composable
fun IrScreen(
    modifier: Modifier = Modifier,
    viewModel: IrViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    IrScreenContent(
        state = state,
        signals = signals,
        moduleInfo = irModuleInfo(state.hasEmitter, state.supportedFrequencies),
        onProtocolChange = viewModel::setProtocol,
        onPayloadChange = viewModel::setPayload,
        onCarrierHzChange = viewModel::setCarrierHz,
        onRepeatsChange = viewModel::setRepeats,
        onTransmit = viewModel::transmit,
        onSave = viewModel::saveSignal,
        onReplay = viewModel::replay,
        onDelete = viewModel::delete,
        onPaste = { clipboard.getText()?.text?.let(viewModel::pasteProto) },
        modifier = modifier,
    )
}

@Composable
private fun irModuleInfo(
    hasEmitter: Boolean,
    supportedFrequencies: List<IntRange>,
): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 19),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.ir_cap_emitter_name),
            detail = stringResource(R.string.ir_cap_emitter_detail),
            status = {
                if (hasEmitter) {
                    val freqText = if (supportedFrequencies.isEmpty()) ""
                    else " · " + supportedFrequencies.joinToString { "${it.first / 1000}–${it.last / 1000} kHz" }
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.ir_cap_emitter_ok) + freqText,
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.ir_cap_emitter_missing),
                    )
                }
            },
        ),
    ),
)

@Composable
internal fun IrScreenContent(
    state: IrState,
    signals: List<IrSignal>,
    moduleInfo: ModuleInfo?,
    onProtocolChange: (IrProtocol) -> Unit,
    onPayloadChange: (String) -> Unit,
    onCarrierHzChange: (Int) -> Unit,
    onRepeatsChange: (Int) -> Unit,
    onTransmit: () -> Unit,
    onSave: (String) -> Unit,
    onReplay: (IrSignal) -> Unit,
    onDelete: (IrSignal) -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.ir_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            IrCapabilityCard(
                hasEmitter = state.hasEmitter,
                supportedFrequencies = state.supportedFrequencies,
            )
            IrSignalComposerCard(
                state = state,
                onProtocolChange = onProtocolChange,
                onPayloadChange = onPayloadChange,
                onCarrierHzChange = onCarrierHzChange,
                onRepeatsChange = onRepeatsChange,
                onTransmit = onTransmit,
                onSave = onSave,
                onPaste = onPaste,
            )
            IrSavedSignalsCard(
                signals = signals,
                onReplay = onReplay,
                onDelete = onDelete,
            )
        },
    )
}

@Composable
private fun IrCapabilityCard(
    hasEmitter: Boolean,
    supportedFrequencies: List<IntRange>,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ir_card_hardware_title),
    ) {
        if (hasEmitter) {
            Text(
                text = stringResource(R.string.ir_emitter_present),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (supportedFrequencies.isNotEmpty()) {
                Text(
                    text = supportedFrequencies.joinToString(" · ") {
                        "${it.first / 1000}–${it.last / 1000} kHz"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.ir_emitter_absent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IrSignalComposerCard(
    state: IrState,
    onProtocolChange: (IrProtocol) -> Unit,
    onPayloadChange: (String) -> Unit,
    onCarrierHzChange: (Int) -> Unit,
    onRepeatsChange: (Int) -> Unit,
    onTransmit: () -> Unit,
    onSave: (String) -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var saveDialogOpen by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var protocolMenuExpanded by remember { mutableStateOf(false) }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ir_card_composer_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            // Protocol dropdown
            ExposedDropdownMenuBox(
                expanded = protocolMenuExpanded,
                onExpandedChange = { protocolMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = state.pendingProtocol.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.ir_label_protocol)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(protocolMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = protocolMenuExpanded,
                    onDismissRequest = { protocolMenuExpanded = false },
                ) {
                    IrProtocol.entries.forEach { proto ->
                        DropdownMenuItem(
                            text = { Text(proto.name) },
                            onClick = { onProtocolChange(proto); protocolMenuExpanded = false },
                        )
                    }
                }
            }

            // Payload + paste
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                OutlinedTextField(
                    value = state.pendingPayload,
                    onValueChange = onPayloadChange,
                    label = {
                        Text(
                            when (state.pendingProtocol) {
                                IrProtocol.NEC -> stringResource(R.string.ir_label_payload_nec)
                                IrProtocol.PRONTO -> stringResource(R.string.ir_label_payload_pronto)
                                IrProtocol.RAW -> stringResource(R.string.ir_label_payload_raw)
                            }
                        )
                    },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPaste) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = stringResource(R.string.ir_btn_paste),
                    )
                }
            }

            // Carrier Hz (hidden for Pronto — auto-derived)
            if (state.pendingProtocol != IrProtocol.PRONTO) {
                OutlinedTextField(
                    value = state.pendingCarrierHz.toString(),
                    onValueChange = { it.toIntOrNull()?.let(onCarrierHzChange) },
                    label = { Text(stringResource(R.string.ir_label_carrier_hz)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Repeats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.ir_label_repeats, state.pendingRepeats),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = state.pendingRepeats.toFloat(),
                    onValueChange = { onRepeatsChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f).padding(start = spacing.small),
                )
            }

            // Result feedback
            when {
                state.lastTransmitOk -> Text(
                    text = stringResource(R.string.ir_transmit_ok),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                state.lastTransmitError != null -> Text(
                    text = state.lastTransmitError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GadgetPrimaryButton(
                    onClick = onTransmit,
                    text = if (state.isTransmitting)
                        stringResource(R.string.ir_transmitting)
                    else
                        stringResource(R.string.ir_btn_transmit),
                    enabled = !state.isTransmitting && state.pendingPayload.isNotBlank(),
                    loading = state.isTransmitting,
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = { saveName = ""; saveDialogOpen = true },
                    text = stringResource(R.string.ir_btn_save),
                    enabled = state.pendingPayload.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (saveDialogOpen) {
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            title = { Text(stringResource(R.string.ir_save_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text(stringResource(R.string.ir_save_dialog_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(saveName)
                        saveDialogOpen = false
                    },
                ) { Text(stringResource(R.string.ir_btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogOpen = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun IrSavedSignalsCard(
    signals: List<IrSignal>,
    onReplay: (IrSignal) -> Unit,
    onDelete: (IrSignal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ir_card_saved_title),
    ) {
        if (signals.isEmpty()) {
            Text(
                text = stringResource(R.string.ir_no_saved_codes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                signals.forEach { signal ->
                    IrSignalRow(signal = signal, onReplay = onReplay, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun IrSignalRow(
    signal: IrSignal,
    onReplay: (IrSignal) -> Unit,
    onDelete: (IrSignal) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = signal.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = signal.protocol.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            IconButton(onClick = { onReplay(signal) }) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.ir_btn_replay))
            }
            IconButton(onClick = { onDelete(signal) }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.ir_btn_delete))
            }
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun IrScreenPreview() = GadgetThemedPreview {
    IrScreenContent(
        state = IrState(
            hasEmitter = true,
            supportedFrequencies = listOf(30_000..56_000),
            pendingProtocol = IrProtocol.NEC,
            pendingPayload = "0x20DF10EF",
            pendingCarrierHz = 38_000,
            pendingRepeats = 1,
        ),
        signals = listOf(
            IrSignal("1", "TV Power", IrProtocol.NEC, "0x20DF10EF", 38_000, 1),
            IrSignal("2", "AC Cool 22°C", IrProtocol.PRONTO, "0000 006C 0022 0002…", 38_000, 1),
        ),
        moduleInfo = null,
        onProtocolChange = {}, onPayloadChange = {}, onCarrierHzChange = {},
        onRepeatsChange = {}, onTransmit = {}, onSave = {}, onReplay = {},
        onDelete = {}, onPaste = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun IrScreenNoEmitterPreview() = GadgetThemedPreview {
    IrScreenContent(
        state = IrState(hasEmitter = false),
        signals = emptyList(),
        moduleInfo = null,
        onProtocolChange = {}, onPayloadChange = {}, onCarrierHzChange = {},
        onRepeatsChange = {}, onTransmit = {}, onSave = {}, onReplay = {},
        onDelete = {}, onPaste = {},
    )
}
