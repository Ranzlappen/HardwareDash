package dev.ranzlappen.gadget.feature.radios.nfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * The config-entry (parameter-bearing) rooted NFC tool (W6 write-tier): a raw
 * NCI command sender. A hex-string [GadgetTextField] feeds the rooted
 * `NfcController.sendRawNciCommand`, gated behind a confirmation
 * [GadgetDialog] because the action `requiresExplicitConfirm` (it drives the
 * NFC controller directly). The no-arg reset lives in the sibling
 * `RootConfirmActionRow`; this card only carries the parameterized action and
 * surfaces the last [RootActionState] (which includes the returned NCI
 * response hex via the ViewModel's mapper).
 *
 * Input is validated to non-empty, even-length hex within the controller's
 * 256-byte payload ceiling before the send button enables; the controller
 * re-validates regardless. Only rendered on the rooted flavor.
 */
@Composable
internal fun NfcToolsCard(
    enabled: Boolean,
    sendNci: RootActionState,
    onSendRawNci: (payloadHex: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var payload by rememberSaveable { mutableStateOf("") }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    val normalized = remember(payload) { payload.filterNot { it.isWhitespace() } }
    val isValidHex = remember(normalized) {
        normalized.isNotEmpty() &&
            normalized.length % 2 == 0 &&
            normalized.length <= MAX_HEX_CHARS &&
            normalized.all { it.digitToIntOrNull(16) != null }
    }
    val showError = payload.isNotBlank() && !isValidHex

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.nfc_tools_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = stringResource(R.string.nfc_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetTextField(
                value = payload,
                onValueChange = { payload = it },
                label = stringResource(R.string.nfc_tools_payload_label),
                placeholder = "00A4040007A0000002471001",
                isError = showError,
                supportingText = if (showError) stringResource(R.string.nfc_tools_payload_error) else null,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = { showConfirm = true },
                text = stringResource(R.string.nfc_tools_send),
                enabled = enabled && isValidHex && !sendNci.running,
                loading = sendNci.running,
                modifier = Modifier.fillMaxWidth(),
            )
            val message = sendNci.message
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = sendNci.statusKind.color(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showConfirm) {
        GadgetDialog(
            onDismissRequest = { showConfirm = false },
            title = stringResource(R.string.nfc_tools_confirm_title),
            text = stringResource(R.string.nfc_tools_confirm_message),
            icon = Icons.Outlined.WarningAmber,
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = {
                        showConfirm = false
                        onSendRawNci(normalized)
                    },
                    text = stringResource(R.string.nfc_tools_send),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { showConfirm = false },
                    text = stringResource(R.string.nfc_root_cancel),
                )
            },
        )
    }
}

/** 256-byte payload ceiling → 512 hex characters. */
private const val MAX_HEX_CHARS = 512

@GadgetPreviewLightDark
@Composable
private fun NfcToolsCardPreview() = GadgetThemedPreview {
    NfcToolsCard(
        enabled = true,
        sendNci = RootActionState(message = "Response: 9000"),
        onSendRawNci = {},
    )
}
