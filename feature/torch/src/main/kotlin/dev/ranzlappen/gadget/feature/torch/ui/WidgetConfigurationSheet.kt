package dev.ranzlappen.gadget.feature.torch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import kotlin.math.roundToInt

/**
 * Modal sheet for creating or editing a [TorchWidgetConfig].
 *
 * Two modes:
 * - **Create** (`isExisting == false`) — the "save" button reads
 *   "Add to home screen" and the title is "New widget". Used from the
 *   in-app "Add flashlight widget" / "Add strobe widget" buttons.
 * - **Edit** (`isExisting == true`) — the "save" button reads "Save"
 *   and the title is "Edit widget". Used from the pencil icon next to
 *   each saved widget in the list.
 *
 * The sheet captures the user's input into local state; only [onConfirm]
 * delivers it to the caller. Cancel / swipe-down dismisses without
 * propagating. State is hoisted to the caller through [onDismiss] /
 * [onConfirm] — the sheet is purely presentational, not stateful
 * across recompositions.
 *
 * For Strobe widgets the sheet exposes:
 * - Rate (Hz) slider — `1f..20f` integer steps.
 * - SOS-mode switch — flag is plumbed but pattern playback is
 *   deferred (see issue #96).
 *
 * For Flashlight widgets only the name field is editable — the variant
 * has no per-instance configurable behaviour today (brightness is
 * tracked at issue #95).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationSheet(
    initial: TorchWidgetConfig,
    isExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TorchWidgetConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initial) { mutableStateOf(initial.displayName) }
    var rateHz by remember(initial) { mutableFloatStateOf(initial.rateHz) }
    var sosMode by remember(initial) { mutableStateOf(initial.sosMode) }

    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        title = stringResource(
            if (isExisting) R.string.torch_widget_config_title_edit
            else R.string.torch_widget_config_title_new,
        ),
    ) {
        GadgetTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.torch_widget_config_name_label),
            modifier = Modifier.fillMaxWidth(),
        )
        if (initial.type == WidgetType.Strobe) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.torch_strobe_rate_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = rateHz,
                    onValueChange = { rateHz = it },
                    valueRange = TorchWidgetConfig.MIN_RATE_HZ..TorchWidgetConfig.MAX_RATE_HZ,
                    steps = (TorchWidgetConfig.MAX_RATE_HZ - TorchWidgetConfig.MIN_RATE_HZ).toInt() - 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.torch_strobe_rate_value,
                        rateHz.roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.torch_widget_config_sos_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(R.string.torch_widget_config_sos_supporting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = sosMode,
                    onCheckedChange = { sosMode = it },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            GadgetTertiaryButton(
                onClick = onDismiss,
                text = stringResource(R.string.torch_widget_config_cancel),
                modifier = Modifier.weight(1f),
            )
            GadgetPrimaryButton(
                onClick = {
                    onConfirm(
                        initial.copy(
                            displayName = name.ifBlank { initial.displayName },
                            rateHz = rateHz,
                            sosMode = sosMode,
                        ),
                    )
                },
                text = stringResource(
                    if (isExisting) R.string.torch_widget_config_save_existing
                    else R.string.torch_widget_config_save_new,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
