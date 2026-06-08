package dev.ranzlappen.gadget.core.widgetkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetColorPicker
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.widgetkit.R
import dev.ranzlappen.gadget.core.widgetkit.config.BackgroundMode
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset

/**
 * The customization dialog for the kit's **content / launcher** widget
 * archetype ([dev.ranzlappen.gadget.core.widgetkit.provider.BaseContentWidgetProvider]
 * consumers) — the parallel to the function-driven [WidgetCustomizationSheet].
 *
 * A content widget paints its own dynamic preview (a folder cover, a media
 * tile, …) and launches an Activity on tap, so it has no function / icon /
 * feedback surface. This sheet exposes only what applies to dynamic content:
 *
 *  1. **Name** — [name].
 *  2. **Content** — the feature's "what does this show" picker ([content] slot).
 *  3. **Background** — [WidgetAppearance.background] + solid colour (shares the
 *     `WidgetAppearanceRenderer.applyBackground` paint path with function
 *     widgets, so the chrome is identical across archetypes).
 *  4. **Accent / content tint** — [tintArgb] (`null` = follow the content's own
 *     natural colour; non-null overrides). The feature decides what "natural"
 *     means.
 *  5. **Label + size** — [showLabel] toggle + [sizePreset] starting-size hint.
 *  6. **Preview** — the feature's live preview ([preview] slot).
 *  7. **Tap animation** — gated behind [showTapAnimation] (default off) until
 *     the content press-frame ships; see `docs/widgets/content-widget-customization.md`.
 *
 * Fully **state-hoisted** — the feature's configure activity owns each value
 * and persists on [onConfirm]. Only [WidgetAppearance.background] /
 * [WidgetAppearance.solidColor] (and [WidgetAppearance.tap] when
 * [showTapAnimation]) are read here; the icon/feedback fields are left at their
 * defaults, so reusing the shared appearance type costs no serialization
 * change.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContentWidgetCustomizationSheet(
    name: String,
    onNameChange: (String) -> Unit,
    appearance: WidgetAppearance,
    onAppearanceChange: (WidgetAppearance) -> Unit,
    tintArgb: Long?,
    onTintChange: (Long?) -> Unit,
    showLabel: Boolean,
    onShowLabelChange: (Boolean) -> Unit,
    sizePreset: WidgetSizePreset,
    onSizePresetChange: (WidgetSizePreset) -> Unit,
    isExisting: Boolean,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    showTapAnimation: Boolean = false,
    content: @Composable () -> Unit,
    preview: @Composable () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
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
            SectionHeader(stringResource(R.string.widget_kit_section_name))
            GadgetTextField(
                value = name,
                onValueChange = onNameChange,
                label = stringResource(R.string.widget_kit_name_label),
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── Content (feature picker) ─────────────────────────────
            SectionHeader(stringResource(R.string.widget_kit_content_section_content))
            content()

            // ─── Background ───────────────────────────────────────────
            SectionHeader(stringResource(R.string.widget_kit_appearance_section_appearance))
            ChipRow(
                label = stringResource(R.string.widget_kit_appearance_background_label),
                selected = appearance.background,
                options = BackgroundMode.values().toList(),
                labelFor = { backgroundModeLabel(it) },
                onSelect = { onAppearanceChange(appearance.copy(background = it)) },
            )
            if (appearance.background == BackgroundMode.Solid) {
                ColorPickerField(
                    label = stringResource(R.string.widget_kit_appearance_solid_color_label),
                    argb = appearance.solidColor,
                    onArgbChange = { onAppearanceChange(appearance.copy(solidColor = it)) },
                )
            }

            // ─── Accent / content tint ────────────────────────────────
            SwitchRow(
                label = stringResource(R.string.widget_kit_content_tint_custom),
                checked = tintArgb != null,
                onCheckedChange = { custom -> onTintChange(if (custom) (tintArgb ?: DEFAULT_TINT_ARGB) else null) },
            )
            if (tintArgb != null) {
                ColorPickerField(
                    label = stringResource(R.string.widget_kit_content_tint_label),
                    argb = tintArgb,
                    onArgbChange = { onTintChange(it) },
                )
            }

            // ─── Label + size ─────────────────────────────────────────
            SwitchRow(
                label = stringResource(R.string.widget_kit_content_label_show),
                checked = showLabel,
                onCheckedChange = onShowLabelChange,
            )
            ChipRow(
                label = stringResource(R.string.widget_kit_content_size_label),
                selected = sizePreset,
                options = WidgetSizePreset.values().toList(),
                labelFor = { sizePresetLabel(it) },
                onSelect = onSizePresetChange,
            )

            // ─── Tap animation (slice 2) ──────────────────────────────
            if (showTapAnimation) {
                ChipRow(
                    label = stringResource(R.string.widget_kit_appearance_tap_animation_label),
                    selected = appearance.tap.animation,
                    options = TapAnimation.values().toList(),
                    labelFor = { tapAnimationLabel(it) },
                    onSelect = {
                        onAppearanceChange(appearance.copy(tap = appearance.tap.copy(animation = it)))
                    },
                )
            }

            // ─── Preview (feature live preview) ───────────────────────
            SectionHeader(stringResource(R.string.widget_kit_appearance_section_preview))
            preview()

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
                    onClick = onConfirm,
                    text = stringResource(
                        if (isExisting) R.string.widget_kit_save else R.string.widget_kit_create,
                    ),
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Default custom accent when the user first flips on "custom colour" — opaque
 *  white, an obvious starting point they then recolour. */
private const val DEFAULT_TINT_ARGB: Long = 0xFFFFFFFFL

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

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
                GadgetChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = labelFor(option),
                )
            }
        }
    }
}

@Composable
private fun ColorPickerField(label: String, argb: Long, onArgbChange: (Long) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GadgetColorPicker(argb = argb, onArgbChange = onArgbChange)
    }
}

@Composable
private fun backgroundModeLabel(mode: BackgroundMode): String = stringResource(
    when (mode) {
        BackgroundMode.GlassSurface -> R.string.widget_kit_appearance_background_glass
        BackgroundMode.Solid -> R.string.widget_kit_appearance_background_solid
        BackgroundMode.Transparent -> R.string.widget_kit_appearance_background_transparent
    },
)

@Composable
private fun sizePresetLabel(preset: WidgetSizePreset): String = stringResource(
    when (preset) {
        WidgetSizePreset.Small -> R.string.widget_kit_content_size_small
        WidgetSizePreset.Medium -> R.string.widget_kit_content_size_medium
        WidgetSizePreset.Large -> R.string.widget_kit_content_size_large
    },
)

@Composable
private fun tapAnimationLabel(animation: TapAnimation): String = stringResource(
    when (animation) {
        TapAnimation.None -> R.string.widget_kit_appearance_tap_animation_none
        TapAnimation.Ripple -> R.string.widget_kit_appearance_tap_animation_ripple
        TapAnimation.Pulse -> R.string.widget_kit_appearance_tap_animation_pulse
        TapAnimation.Scale -> R.string.widget_kit_appearance_tap_animation_scale
        TapAnimation.Flash -> R.string.widget_kit_appearance_tap_animation_flash
    },
)
