package dev.ranzlappen.gadget.core.widgetkit.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetColorPicker
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.widgetkit.R
import dev.ranzlappen.gadget.core.widgetkit.config.BackgroundMode
import dev.ranzlappen.gadget.core.widgetkit.config.IconTint
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.ToggleFeedback
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconKeys
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.render.iconTintArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The generic appearance / tap / feedback / preview UI block every
 * widget-bearing feature drops into its configuration sheet.
 *
 * Drives [WidgetAppearance] edits (background mode + solid color, icon
 * style + tint + custom-tint color, tap behaviour + animation, toggle
 * feedback variant + templates) and renders a live
 * [WidgetAppearancePreview]. State is hoisted: [appearance] is the
 * current value, [onAppearanceChange] fires on every edit.
 *
 * Lifted from torch's `WidgetConfigurationSheet` in refactor-2026
 * Phase 2 / C6b so every widget-bearing feature shares one localised
 * implementation. Torch's sheet now composes this section + its own
 * torch-specific fields (name + rate + Morse) around it.
 *
 * @param appearance current value, hoisted by the feature's sheet.
 * @param onAppearanceChange fires on every edit.
 * @param iconChoices the feature's bundled icon swatches (mapped from
 *                    its catalog).
 * @param resolveIcon the feature's resolver — translates an icon key
 *                    (built-in or custom-prefixed) to its
 *                    [WidgetIconSource].
 * @param onImportCustomIcon the feature's importer — opens the system
 *                            file picker, copies + downscales the
 *                            chosen image into app storage, returns
 *                            the new custom-icon key.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetAppearanceSection(
    appearance: WidgetAppearance,
    onAppearanceChange: (WidgetAppearance) -> Unit,
    iconChoices: List<WidgetIconChoice>,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    // Pre-resolve the default feedback templates here, in @Composable
    // context, so the FeedbackKind chip's onSelect callback (which
    // isn't @Composable) can build a fresh ToggleFeedback payload
    // without re-calling stringResource.
    val defaultTemplates = DefaultFeedbackTemplates(
        toast = stringResource(R.string.widget_kit_appearance_feedback_default_toast_template),
        notificationTitle = stringResource(R.string.widget_kit_appearance_feedback_default_notification_title),
        notificationBody = stringResource(R.string.widget_kit_appearance_feedback_default_notification_body),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        // ─── Appearance ───────────────────────────────────────────
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
        ChipRow(
            label = stringResource(R.string.widget_kit_appearance_tint_label),
            selected = appearance.iconStyle.tint,
            options = IconTint.values().toList(),
            labelFor = { iconTintLabel(it) },
            onSelect = {
                onAppearanceChange(
                    appearance.copy(iconStyle = appearance.iconStyle.copy(tint = it)),
                )
            },
        )
        if (appearance.iconStyle.tint == IconTint.Custom) {
            ColorPickerField(
                label = stringResource(R.string.widget_kit_appearance_tint_custom_color),
                argb = appearance.iconStyle.customTintArgb,
                onArgbChange = {
                    onAppearanceChange(
                        appearance.copy(iconStyle = appearance.iconStyle.copy(customTintArgb = it)),
                    )
                },
            )
        }
        val iconTint = iconTintArgb(LocalContext.current, appearance.iconStyle)
        IconPickerRow(
            label = stringResource(R.string.widget_kit_appearance_icon_active_label),
            selectedKey = appearance.iconStyle.activeKey,
            choices = iconChoices,
            tintArgb = iconTint,
            resolveIcon = resolveIcon,
            onImportCustomIcon = onImportCustomIcon,
            onSelect = { key ->
                onAppearanceChange(
                    appearance.copy(iconStyle = appearance.iconStyle.copy(activeKey = key)),
                )
            },
        )
        IconPickerRow(
            label = stringResource(R.string.widget_kit_appearance_icon_inactive_label),
            selectedKey = appearance.iconStyle.inactiveKey,
            choices = iconChoices,
            tintArgb = iconTint,
            resolveIcon = resolveIcon,
            onImportCustomIcon = onImportCustomIcon,
            onSelect = { key ->
                onAppearanceChange(
                    appearance.copy(iconStyle = appearance.iconStyle.copy(inactiveKey = key)),
                )
            },
        )

        // ─── Tap behaviour ────────────────────────────────────────
        SectionHeader(stringResource(R.string.widget_kit_appearance_section_tap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.widget_kit_appearance_tap_enabled_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.widget_kit_appearance_tap_disabled_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = appearance.tap.enabled,
                onCheckedChange = {
                    onAppearanceChange(
                        appearance.copy(tap = appearance.tap.copy(enabled = it)),
                    )
                },
            )
        }
        ChipRow(
            label = stringResource(R.string.widget_kit_appearance_tap_animation_label),
            selected = appearance.tap.animation,
            options = TapAnimation.values().toList(),
            labelFor = { tapAnimationLabel(it) },
            onSelect = {
                onAppearanceChange(
                    appearance.copy(tap = appearance.tap.copy(animation = it)),
                )
            },
        )

        // ─── Toggle feedback ──────────────────────────────────────
        SectionHeader(stringResource(R.string.widget_kit_appearance_section_feedback))
        val feedbackKind = remember(appearance.feedback) {
            when (appearance.feedback) {
                ToggleFeedback.None -> FeedbackKind.None
                is ToggleFeedback.Toast -> FeedbackKind.Toast
                is ToggleFeedback.Notification -> FeedbackKind.Notification
            }
        }
        ChipRow(
            label = stringResource(R.string.widget_kit_appearance_feedback_kind_label),
            selected = feedbackKind,
            options = FeedbackKind.values().toList(),
            labelFor = { feedbackKindLabel(it) },
            onSelect = { kind ->
                onAppearanceChange(appearance.copy(feedback = kind.defaultPayload(defaultTemplates)))
            },
        )
        FeedbackTemplateFields(
            feedback = appearance.feedback,
            onFeedbackChange = { onAppearanceChange(appearance.copy(feedback = it)) },
        )

        // ─── Live preview ─────────────────────────────────────────
        SectionHeader(stringResource(R.string.widget_kit_appearance_section_preview))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WidgetAppearancePreview(
                appearance = appearance,
                icon = resolveIcon(appearance.iconStyle.activeKey),
                interactive = true,
            )
        }
        if (appearance.tap.enabled && appearance.tap.animation != TapAnimation.None) {
            Text(
                text = stringResource(R.string.widget_kit_appearance_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
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

/** Fixed swatch geometry. 48 dp is the Material accessibility minimum
 *  touch target. */
private object SwatchDefaults {
    val Diameter = 48.dp
    val SelectedRing = 3.dp
    val UnselectedRing = 1.dp
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerRow(
    label: String,
    selectedKey: String,
    choices: List<WidgetIconChoice>,
    tintArgb: Int,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    onSelect: (String) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val scope = rememberCoroutineScope()
    var importError by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            importError = false
            scope.launch {
                val key = onImportCustomIcon(uri)
                if (key != null) onSelect(key) else importError = true
            }
        }
    }
    val customSelected = selectedKey.startsWith(WidgetIconKeys.CUSTOM_PREFIX)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
        ) {
            choices.forEach { entry ->
                IconSwatch(
                    source = WidgetIconSource.Resource(entry.drawable),
                    tintArgb = tintArgb,
                    selected = entry.key == selectedKey,
                    contentDescription = entry.displayName,
                    onClick = { onSelect(entry.key) },
                )
            }
            if (customSelected) {
                IconSwatch(
                    source = resolveIcon(selectedKey),
                    tintArgb = null,
                    selected = true,
                    contentDescription = stringResource(R.string.widget_kit_appearance_icon_custom),
                    onClick = { picker.launch(IMAGE_MIME_TYPE) },
                )
            }
            AddCustomSwatch(onClick = { picker.launch(IMAGE_MIME_TYPE) })
        }
        if (importError) {
            Text(
                text = stringResource(R.string.widget_kit_appearance_icon_import_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun IconSwatch(
    source: WidgetIconSource,
    tintArgb: Int?,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Box(
        modifier = Modifier
            .defaultMinSize(SwatchDefaults.Diameter, SwatchDefaults.Diameter)
            .size(SwatchDefaults.Diameter)
            .clip(CircleShape)
            .border(
                width = if (selected) SwatchDefaults.SelectedRing else SwatchDefaults.UnselectedRing,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(role = Role.RadioButton, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        when (source) {
            is WidgetIconSource.Resource -> Image(
                painter = painterResource(source.resId),
                contentDescription = contentDescription,
                colorFilter = tintArgb?.let { ColorFilter.tint(Color(it)) },
                modifier = Modifier.fillMaxSize(),
            )
            is WidgetIconSource.CustomFile -> {
                val bitmap by produceState<ImageBitmap?>(initialValue = null, source.path) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { BitmapFactory.decodeFile(source.path)?.asImageBitmap() }.getOrNull()
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomSwatch(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(SwatchDefaults.Diameter, SwatchDefaults.Diameter)
            .size(SwatchDefaults.Diameter)
            .clip(CircleShape)
            .border(SwatchDefaults.UnselectedRing, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.widget_kit_appearance_icon_add_custom),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.widget_kit_appearance_icon_add_custom),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FeedbackTemplateFields(
    feedback: ToggleFeedback,
    onFeedbackChange: (ToggleFeedback) -> Unit,
) {
    when (feedback) {
        ToggleFeedback.None -> Unit
        is ToggleFeedback.Toast -> {
            GadgetTextField(
                value = feedback.template,
                onValueChange = { onFeedbackChange(ToggleFeedback.Toast(it)) },
                label = stringResource(R.string.widget_kit_appearance_feedback_toast_template_label),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.widget_kit_appearance_feedback_template_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is ToggleFeedback.Notification -> {
            GadgetTextField(
                value = feedback.titleTemplate,
                onValueChange = { onFeedbackChange(feedback.copy(titleTemplate = it)) },
                label = stringResource(R.string.widget_kit_appearance_feedback_notification_title_label),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetTextField(
                value = feedback.bodyTemplate,
                onValueChange = { onFeedbackChange(feedback.copy(bodyTemplate = it)) },
                label = stringResource(R.string.widget_kit_appearance_feedback_notification_body_label),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.widget_kit_appearance_feedback_template_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val IMAGE_MIME_TYPE = "image/*"

private enum class FeedbackKind { None, Toast, Notification }

/** Default template strings the FeedbackKind chip's onSelect callback
 *  uses to populate a fresh [ToggleFeedback]. Pre-resolved in @Composable
 *  context by the [WidgetAppearanceSection] caller; carried into the
 *  click callback as plain strings. */
private data class DefaultFeedbackTemplates(
    val toast: String,
    val notificationTitle: String,
    val notificationBody: String,
)

private fun FeedbackKind.defaultPayload(
    templates: DefaultFeedbackTemplates,
): ToggleFeedback = when (this) {
    FeedbackKind.None -> ToggleFeedback.None
    FeedbackKind.Toast -> ToggleFeedback.Toast(template = templates.toast)
    FeedbackKind.Notification -> ToggleFeedback.Notification(
        titleTemplate = templates.notificationTitle,
        bodyTemplate = templates.notificationBody,
    )
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
private fun iconTintLabel(tint: IconTint): String = stringResource(
    when (tint) {
        IconTint.ThemeAccent -> R.string.widget_kit_appearance_tint_accent
        IconTint.ThemeOnSurface -> R.string.widget_kit_appearance_tint_on_surface
        IconTint.MonochromeWhite -> R.string.widget_kit_appearance_tint_white
        IconTint.MonochromeBlack -> R.string.widget_kit_appearance_tint_black
        IconTint.Custom -> R.string.widget_kit_appearance_tint_custom
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

@Composable
private fun feedbackKindLabel(kind: FeedbackKind): String = stringResource(
    when (kind) {
        FeedbackKind.None -> R.string.widget_kit_appearance_feedback_none
        FeedbackKind.Toast -> R.string.widget_kit_appearance_feedback_toast
        FeedbackKind.Notification -> R.string.widget_kit_appearance_feedback_notification
    },
)
