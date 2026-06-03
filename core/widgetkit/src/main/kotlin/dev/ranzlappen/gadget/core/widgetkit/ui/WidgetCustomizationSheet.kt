package dev.ranzlappen.gadget.core.widgetkit.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.widgetkit.R
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionBehavior
import kotlin.math.roundToInt

/** The result of confirming the customization dialog — everything a feature
 *  needs to build/update its widget config. */
data class WidgetCustomizationResult(
    val name: String,
    val actionKey: String,
    val params: Map<String, String>,
    val sizePreset: WidgetSizePreset,
    val appearance: WidgetAppearance,
)

/**
 * The single comprehensive widget-customization dialog every widget-bearing
 * feature opens from its one "Add widget" button. Generic over the feature's
 * [functions] + icon catalog; a feature wraps it in a thin shell that maps its
 * config in and out.
 *
 * Sections, top to bottom: **Name → Function → Settings (per-function params)
 * → Size → Appearance ([WidgetAppearanceSection]) → live Preview** (the
 * preview lives inside the appearance section).
 *
 * - [functions] is already flavor-filtered by the caller (a `requiresRoot`
 *   function is dropped on standard), so the picker only ever lists runnable
 *   functions.
 * - Params render automatically from each [WidgetFunction.params]
 *   ([ActionParam]): Int/Float → [GadgetSlider], Bool → [Switch], Text →
 *   [GadgetTextField]. A feature can override the editor for a specific param
 *   name via [paramOverrides] (e.g. vibration's `pattern_id` saved-pattern
 *   chooser).
 * - The icon picker shows active+inactive for a toggle function and a single
 *   icon for a momentary one (derived from the selected function's behavior).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetCustomizationSheet(
    initialName: String,
    initialActionKey: String,
    initialParams: Map<String, String>,
    initialSizePreset: WidgetSizePreset,
    initialAppearance: WidgetAppearance,
    functions: List<WidgetFunction>,
    isExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (WidgetCustomizationResult) -> Unit,
    iconChoices: List<WidgetIconChoice>,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    modifier: Modifier = Modifier,
    paramOverrides: Map<String, @Composable (value: String, onChange: (String) -> Unit) -> Unit> = emptyMap(),
) {
    val spacing = LocalGadgetTheme.current.spacing

    var name by remember { mutableStateOf(initialName) }
    var selectedId by remember {
        mutableStateOf(
            initialActionKey.takeIf { key -> functions.any { it.id == key } }
                ?: functions.firstOrNull()?.id.orEmpty(),
        )
    }
    var sizePreset by remember { mutableStateOf(initialSizePreset) }
    var appearance by remember { mutableStateOf(initialAppearance) }

    val function = functions.firstOrNull { it.id == selectedId } ?: functions.firstOrNull()

    // Per-function param values, re-seeded whenever the selected function
    // changes: from the persisted values for the initially-selected function,
    // from defaults for any newly-picked one.
    val params = remember(selectedId) {
        mutableStateMapOf<String, String>().apply {
            function?.params?.forEach { param ->
                put(
                    param.name,
                    if (selectedId == initialActionKey) {
                        initialParams[param.name] ?: param.default
                    } else {
                        param.default
                    },
                )
            }
        }
    }

    val iconMode = when (function?.behavior) {
        is WidgetFunctionBehavior.Toggle -> WidgetIconPickerMode.ActiveInactive
        else -> WidgetIconPickerMode.Single
    }

    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(
            if (isExisting) R.string.widget_kit_dialog_title_edit else R.string.widget_kit_dialog_title_new,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            // ─── Name ─────────────────────────────────────────────────
            SheetSectionHeader(stringResource(R.string.widget_kit_section_name))
            GadgetTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.widget_kit_name_label),
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── Function ─────────────────────────────────────────────
            if (functions.size > 1) {
                SheetSectionHeader(stringResource(R.string.widget_kit_section_function))
                LabeledChipRow(label = stringResource(R.string.widget_kit_function_label)) {
                    functions.forEach { fn ->
                        GadgetChip(
                            selected = fn.id == selectedId,
                            onClick = { selectedId = fn.id },
                            label = fn.label,
                        )
                    }
                }
            }

            // ─── Per-function settings (params) ───────────────────────
            val visibleParams = function?.params.orEmpty()
            if (visibleParams.isNotEmpty()) {
                SheetSectionHeader(stringResource(R.string.widget_kit_section_params))
                visibleParams.forEach { param ->
                    ParamEditor(
                        param = param,
                        value = params[param.name] ?: param.default,
                        onValueChange = { params[param.name] = it },
                        override = paramOverrides[param.name],
                    )
                }
            }

            // ─── Size ─────────────────────────────────────────────────
            SheetSectionHeader(stringResource(R.string.widget_kit_section_size))
            LabeledChipRow(label = stringResource(R.string.widget_kit_size_label)) {
                WidgetSizePreset.values().forEach { preset ->
                    GadgetChip(
                        selected = preset == sizePreset,
                        onClick = { sizePreset = preset },
                        label = sizePresetLabel(preset),
                    )
                }
            }
            Text(
                text = stringResource(R.string.widget_kit_size_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ─── Appearance + preview ─────────────────────────────────
            WidgetAppearanceSection(
                appearance = appearance,
                onAppearanceChange = { appearance = it },
                iconChoices = iconChoices,
                resolveIcon = resolveIcon,
                onImportCustomIcon = onImportCustomIcon,
                iconMode = iconMode,
            )

            // ─── Footer ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetTertiaryButton(
                    onClick = onDismiss,
                    text = stringResource(R.string.widget_kit_cancel),
                    modifier = Modifier.weight(1f),
                )
                GadgetPrimaryButton(
                    onClick = {
                        onConfirm(
                            WidgetCustomizationResult(
                                name = name,
                                actionKey = selectedId,
                                params = params.toMap(),
                                sizePreset = sizePreset,
                                appearance = appearance,
                            ),
                        )
                    },
                    text = stringResource(
                        if (isExisting) R.string.widget_kit_save else R.string.widget_kit_create,
                    ),
                    enabled = function != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ParamEditor(
    param: ActionParam,
    value: String,
    onValueChange: (String) -> Unit,
    override: (@Composable (value: String, onChange: (String) -> Unit) -> Unit)?,
) {
    if (override != null) {
        override(value, onValueChange)
        return
    }
    when (param.type) {
        ActionParamType.Int, ActionParamType.Float -> {
            val isInt = param.type == ActionParamType.Int
            val min = param.min ?: 0f
            val max = param.max ?: 100f
            val current = value.toFloatOrNull() ?: param.default.toFloatOrNull() ?: min
            GadgetSlider(
                value = current.coerceIn(min, max),
                onValueChange = { raw ->
                    onValueChange(if (isInt) raw.roundToInt().toString() else raw.toString())
                },
                valueRange = min..max,
                label = param.name.toLabel(),
                valueFormatter = { if (isInt) it.roundToInt().toString() else String.format("%.1f", it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ActionParamType.Bool -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = param.name.toLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = value.toBooleanStrictOrNull() ?: param.default.toBooleanStrictOrNull() ?: false,
                    onCheckedChange = { onValueChange(it.toString()) },
                )
            }
        }
        ActionParamType.Text -> {
            GadgetTextField(
                value = value,
                onValueChange = onValueChange,
                label = param.name.toLabel(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SheetSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabeledChipRow(label: String, content: @Composable FlowRowScope.() -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
            content = content,
        )
    }
}

@Composable
private fun sizePresetLabel(preset: WidgetSizePreset): String = stringResource(
    when (preset) {
        WidgetSizePreset.Small -> R.string.widget_kit_size_small
        WidgetSizePreset.Medium -> R.string.widget_kit_size_medium
        WidgetSizePreset.Large -> R.string.widget_kit_size_large
    },
)

/** Turn a snake_case param name into a readable label ("rate_hz" → "Rate hz").
 *  Param labels are developer-facing keys; this keeps the dialog tidy without
 *  forcing a per-param localized string for every feature. */
private fun String.toLabel(): String =
    replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
