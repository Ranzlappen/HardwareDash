package dev.ranzlappen.gadget.feature.torch.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetAppearanceSection
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType

/**
 * Modal sheet for creating or editing a [TorchWidgetConfig].
 *
 * Captures the torch-specific surface (name + strobe-only fields) and
 * delegates everything generic — background mode, icon style, tint, tap
 * behaviour, toggle feedback, live preview — to the kit-side
 * [WidgetAppearanceSection]. The kit section owns its own labels +
 * helper composables (chip rows, color picker fields, icon swatches,
 * the add-custom-icon flow), so this file shrinks to the torch-only
 * shell.
 *
 * State is hoisted to local `remember`d variables, keyed by [initial]
 * so the sheet rebuilds cleanly when switching between widgets. Only
 * [onConfirm] propagates the captured values to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationSheet(
    initial: TorchWidgetConfig,
    isExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TorchWidgetConfig) -> Unit,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    iconChoices: List<WidgetIconChoice>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initial) { mutableStateOf(initial.displayName) }
    var rateHz by remember(initial) { mutableFloatStateOf(initial.rateHz) }
    var morseMode by remember(initial) { mutableStateOf(initial.morseMode) }
    var morseText by remember(initial) { mutableStateOf(initial.morseText) }
    var appearance by remember(initial) { mutableStateOf(initial.appearance) }

    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        title = stringResource(
            if (isExisting) R.string.torch_widget_config_title_edit
            else R.string.torch_widget_config_title_new,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            // ─── Torch-specific fields ────────────────────────────
            GadgetTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.torch_widget_config_name_label),
                modifier = Modifier.fillMaxWidth(),
            )
            if (initial.type == WidgetType.Strobe) {
                StrobeSpecificFields(
                    rateHz = rateHz,
                    onRateChange = { rateHz = it },
                    morseMode = morseMode,
                    onMorseModeChange = { morseMode = it },
                    morseText = morseText,
                    onMorseTextChange = { morseText = it },
                )
            }

            // ─── Kit-generic appearance / tap / feedback / preview ──
            WidgetAppearanceSection(
                appearance = appearance,
                onAppearanceChange = { appearance = it },
                iconChoices = iconChoices,
                resolveIcon = resolveIcon,
                onImportCustomIcon = onImportCustomIcon,
            )

            // ─── Footer actions ──────────────────────────────────
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
                                morseMode = morseMode,
                                morseText = morseText,
                                appearance = appearance,
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
}

@Composable
private fun StrobeSpecificFields(
    rateHz: Float,
    onRateChange: (Float) -> Unit,
    morseMode: Boolean,
    onMorseModeChange: (Boolean) -> Unit,
    morseText: String,
    onMorseTextChange: (String) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetSlider(
        value = rateHz,
        onValueChange = onRateChange,
        valueRange = TorchWidgetConfig.MIN_RATE_HZ..TorchWidgetConfig.MAX_RATE_HZ,
        steps = (TorchWidgetConfig.MAX_RATE_HZ - TorchWidgetConfig.MIN_RATE_HZ).toInt() - 1,
        label = stringResource(R.string.torch_strobe_rate_label),
        suffix = "Hz",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.torch_widget_config_morse_mode_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.torch_widget_config_morse_mode_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = morseMode, onCheckedChange = onMorseModeChange)
    }
    GadgetTextField(
        value = morseText,
        onValueChange = onMorseTextChange,
        label = stringResource(R.string.torch_widget_config_morse_label),
        supportingText = stringResource(R.string.torch_widget_config_morse_supporting),
        modifier = Modifier.fillMaxWidth(),
    )
}
