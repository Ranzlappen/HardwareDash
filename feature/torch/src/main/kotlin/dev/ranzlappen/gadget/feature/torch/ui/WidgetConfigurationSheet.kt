package dev.ranzlappen.gadget.feature.torch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import dev.ranzlappen.gadget.feature.torch.widget.customization.BackgroundMode
import dev.ranzlappen.gadget.feature.torch.widget.customization.IconTint
import dev.ranzlappen.gadget.feature.torch.widget.customization.TapAnimation
import dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback

/**
 * Modal sheet for creating or editing a [TorchWidgetConfig].
 *
 * Captures every customisation surface for a single widget:
 *  - Name
 *  - (Strobe only) Rate Hz + SOS mode
 *  - Appearance: background mode + (light) icon style + tint
 *  - Tap behaviour: animation + enabled flag
 *  - Toggle feedback: kind (None/Toast/Notification) + templates
 *
 * State is hoisted to local `remember`d variables, keyed by [initial]
 * so the sheet rebuilds cleanly when switching between widgets in
 * the in-app list. Only [onConfirm] propagates the captured values
 * to the caller; cancel / swipe-down dismisses silently.
 *
 * Pickers are built from plain Material 3 widgets (`FilterChip` rows
 * acting as segmented selectors, `Switch`, `OutlinedTextField`) so
 * the sheet stays self-contained without needing the wider
 * design-system picker primitives that future batches will build out.
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
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            GadgetTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.torch_widget_config_name_label),
                modifier = Modifier.fillMaxWidth(),
            )

            if (initial.type == WidgetType.Strobe) {
                GadgetSlider(
                    value = rateHz,
                    onValueChange = { rateHz = it },
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
                            text = stringResource(R.string.torch_widget_config_sos_label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = stringResource(R.string.torch_widget_config_sos_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = sosMode, onCheckedChange = { sosMode = it })
                }
            }

            // ─── Appearance section ──────────────────────────────────
            SheetSectionHeader(stringResource(R.string.torch_widget_config_section_appearance))
            ChipRow(
                label = stringResource(R.string.torch_widget_config_background_label),
                selected = appearance.background,
                options = BackgroundMode.values().toList(),
                labelFor = { backgroundModeLabel(it) },
                onSelect = { appearance = appearance.copy(background = it) },
            )
            ChipRow(
                label = stringResource(R.string.torch_widget_config_tint_label),
                selected = appearance.iconStyle.tint,
                options = IconTint.values().toList(),
                labelFor = { iconTintLabel(it) },
                onSelect = {
                    appearance = appearance.copy(
                        iconStyle = appearance.iconStyle.copy(tint = it),
                    )
                },
            )

            // ─── Tap behaviour section ───────────────────────────────
            SheetSectionHeader(stringResource(R.string.torch_widget_config_section_tap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.torch_widget_config_tap_enabled_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(R.string.torch_widget_config_tap_disabled_supporting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = appearance.tap.enabled,
                    onCheckedChange = {
                        appearance = appearance.copy(
                            tap = appearance.tap.copy(enabled = it),
                        )
                    },
                )
            }
            ChipRow(
                label = stringResource(R.string.torch_widget_config_tap_animation_label),
                selected = appearance.tap.animation,
                options = TapAnimation.values().toList(),
                labelFor = { tapAnimationLabel(it) },
                onSelect = {
                    appearance = appearance.copy(
                        tap = appearance.tap.copy(animation = it),
                    )
                },
            )

            // ─── Toggle feedback section ─────────────────────────────
            SheetSectionHeader(stringResource(R.string.torch_widget_config_section_feedback))
            val feedbackKind = remember(appearance.feedback) {
                when (appearance.feedback) {
                    ToggleFeedback.None -> FeedbackKind.None
                    is ToggleFeedback.Toast -> FeedbackKind.Toast
                    is ToggleFeedback.Notification -> FeedbackKind.Notification
                }
            }
            ChipRow(
                label = stringResource(R.string.torch_widget_config_feedback_kind_label),
                selected = feedbackKind,
                options = FeedbackKind.values().toList(),
                labelFor = { feedbackKindLabel(it) },
                onSelect = { kind ->
                    appearance = appearance.copy(feedback = kind.defaultPayload())
                },
            )
            when (val feedback = appearance.feedback) {
                ToggleFeedback.None -> Unit
                is ToggleFeedback.Toast -> {
                    GadgetTextField(
                        value = feedback.template,
                        onValueChange = {
                            appearance = appearance.copy(
                                feedback = ToggleFeedback.Toast(it),
                            )
                        },
                        label = stringResource(R.string.torch_widget_config_feedback_toast_template_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.torch_widget_config_feedback_template_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ToggleFeedback.Notification -> {
                    GadgetTextField(
                        value = feedback.titleTemplate,
                        onValueChange = {
                            appearance = appearance.copy(
                                feedback = feedback.copy(titleTemplate = it),
                            )
                        },
                        label = stringResource(R.string.torch_widget_config_feedback_notification_title_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GadgetTextField(
                        value = feedback.bodyTemplate,
                        onValueChange = {
                            appearance = appearance.copy(
                                feedback = feedback.copy(bodyTemplate = it),
                            )
                        },
                        label = stringResource(R.string.torch_widget_config_feedback_notification_body_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.torch_widget_config_feedback_template_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ─── Footer actions ──────────────────────────────────────
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
private fun SheetSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Light-weight segmented selector built from a flow of M3
 * [FilterChip] widgets. Chips wrap to a new line when their
 * combined intrinsic width exceeds the row — the previous rigid
 * [Row] squeezed the trailing chip until its label wrapped
 * vertically (e.g. the four-option Tint row truncating "Black" to
 * "Bl/ac/k"). [FlowRow] reflows instead of clipping. Used until
 * the design-system `GadgetSegmentedSelector` primitive ships.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    label: String,
    selected: T,
    options: List<T>,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
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
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(text = labelFor(option), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}

/** Compact discriminator for the feedback-kind chip row. The real
 *  payload is built lazily on selection via [FeedbackKind
 *  .defaultPayload]. */
private enum class FeedbackKind { None, Toast, Notification }

private fun FeedbackKind.defaultPayload(): ToggleFeedback = when (this) {
    FeedbackKind.None -> ToggleFeedback.None
    FeedbackKind.Toast -> ToggleFeedback.Toast(template = DEFAULT_TOAST_TEMPLATE)
    FeedbackKind.Notification -> ToggleFeedback.Notification(
        titleTemplate = DEFAULT_NOTIFICATION_TITLE,
        bodyTemplate = DEFAULT_NOTIFICATION_BODY,
    )
}

@Composable
private fun backgroundModeLabel(mode: BackgroundMode): String = stringResource(
    when (mode) {
        BackgroundMode.GlassSurface -> R.string.torch_widget_config_background_glass
        BackgroundMode.Solid -> R.string.torch_widget_config_background_solid
        BackgroundMode.Transparent -> R.string.torch_widget_config_background_transparent
    },
)

@Composable
private fun iconTintLabel(tint: IconTint): String = stringResource(
    when (tint) {
        IconTint.ThemeAccent -> R.string.torch_widget_config_tint_accent
        IconTint.ThemeOnSurface -> R.string.torch_widget_config_tint_on_surface
        IconTint.MonochromeWhite -> R.string.torch_widget_config_tint_white
        IconTint.MonochromeBlack -> R.string.torch_widget_config_tint_black
        IconTint.Custom -> R.string.torch_widget_config_tint_custom
    },
)

@Composable
private fun tapAnimationLabel(animation: TapAnimation): String = stringResource(
    when (animation) {
        TapAnimation.None -> R.string.torch_widget_config_tap_animation_none
        TapAnimation.Ripple -> R.string.torch_widget_config_tap_animation_ripple
        TapAnimation.Pulse -> R.string.torch_widget_config_tap_animation_pulse
        TapAnimation.Scale -> R.string.torch_widget_config_tap_animation_scale
        TapAnimation.Flash -> R.string.torch_widget_config_tap_animation_flash
    },
)

@Composable
private fun feedbackKindLabel(kind: FeedbackKind): String = stringResource(
    when (kind) {
        FeedbackKind.None -> R.string.torch_widget_config_feedback_none
        FeedbackKind.Toast -> R.string.torch_widget_config_feedback_toast
        FeedbackKind.Notification -> R.string.torch_widget_config_feedback_notification
    },
)

private const val DEFAULT_TOAST_TEMPLATE = "{name} is now {state}"
private const val DEFAULT_NOTIFICATION_TITLE = "{name}"
private const val DEFAULT_NOTIFICATION_BODY = "Now {state}"
