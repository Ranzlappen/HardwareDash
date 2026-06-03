package dev.ranzlappen.gadget.feature.vibration.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationResult
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationSheet
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationPattern
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig

/**
 * Thin vibration wrapper over the kit's generic [WidgetCustomizationSheet]. It
 * supplies the feature seams (functions, icon catalog, custom-icon import) and
 * adds exactly one feature-specific control: a saved-pattern chooser rendered in
 * place of the default `pattern_id` text field (via [WidgetCustomizationSheet]'s
 * `paramOverrides`). Everything else — name, function picker, the auto-generated
 * one-shot amplitude/duration sliders, size, appearance, and the live preview —
 * is owned by the kit. Mirror of torch's `WidgetConfigurationSheet`.
 */
@Composable
internal fun WidgetConfigurationSheet(
    initial: VibrationWidgetConfig,
    isExisting: Boolean,
    functions: List<WidgetFunction>,
    savedPatterns: List<VibrationPattern>,
    onDismiss: () -> Unit,
    onConfirm: (WidgetCustomizationResult) -> Unit,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    iconChoices: List<WidgetIconChoice>,
    modifier: Modifier = Modifier,
) {
    WidgetCustomizationSheet(
        initialName = initial.displayName,
        initialActionKey = initial.actionKey,
        initialParams = initial.params,
        initialSizePreset = initial.sizePreset,
        initialAppearance = initial.appearance,
        functions = functions,
        isExisting = isExisting,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        iconChoices = iconChoices,
        resolveIcon = resolveIcon,
        onImportCustomIcon = onImportCustomIcon,
        modifier = modifier,
        paramOverrides = mapOf(
            VibrationActionHandler.PARAM_PATTERN_ID to { value, onChange ->
                SavedPatternChooser(
                    patterns = savedPatterns,
                    selectedId = value,
                    onSelect = onChange,
                )
            },
        ),
    )
}

/**
 * The `pattern_id` param editor — a chip row of the user's saved patterns
 * instead of a raw text field. Shows a hint when there are no patterns yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPatternChooser(
    patterns: List<VibrationPattern>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Text(
            text = stringResource(R.string.vibration_widget_config_pattern_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (patterns.isEmpty()) {
            Text(
                text = stringResource(R.string.vibration_widget_config_pattern_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            ) {
                patterns.forEach { pattern ->
                    GadgetChip(
                        selected = pattern.id == selectedId,
                        onClick = { onSelect(pattern.id) },
                        label = pattern.name,
                    )
                }
            }
        }
    }
}
