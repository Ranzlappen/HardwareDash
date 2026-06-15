package dev.ranzlappen.gadget.feature.radios.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
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
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun NfcScreen(
    modifier: Modifier = Modifier,
    viewModel: NfcViewModel = hiltViewModel(),
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
    NfcScreenContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is NfcUiEvent.SetHcePayload -> viewModel.setHcePayload(event.text)
                is NfcUiEvent.ActivateHce -> viewModel.activateHce(event.mode)
                NfcUiEvent.ClearHce -> viewModel.clearHce()
            }
        },
        modifier = modifier,
    )
}

sealed interface NfcUiEvent {
    data class SetHcePayload(val text: String) : NfcUiEvent
    data class ActivateHce(val mode: NfcHceMode) : NfcUiEvent
    data object ClearHce : NfcUiEvent
}

@Composable
fun NfcScreenContent(
    state: NfcState,
    onEvent: (NfcUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        moduleInfo = ModuleInfo(
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
        ),
        modifier = modifier,
        functional = {
            NfcCapabilityCard(state = state)
            NfcTagCard(state = state)
            NfcHceCard(state = state, onEvent = onEvent)
            MonitorContainer(
                metricKey = NfcEnabledMetricSource.METRIC_KEY,
                title = stringResource(R.string.nfc_monitor_title),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "nfc_monitor",
            )
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
