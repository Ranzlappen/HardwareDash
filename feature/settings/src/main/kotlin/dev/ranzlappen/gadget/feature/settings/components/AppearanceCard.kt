package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.datastore.CustomPalette
import dev.ranzlappen.gadget.core.datastore.CustomThemeOption
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetCustomTheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetDarkColorScheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetLightColorScheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.colorScheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetColorPicker
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.settings.R

/**
 * Appearance card — dark-theme mode selector + dynamic-color
 * toggle. State hoisted: the parent ([SettingsScreen]) passes
 * [preferences] + the two setter callbacks.
 *
 * Dark theme is exposed as three [GadgetChip]s (Light / Dark /
 * Follow system) rather than a binary switch so the
 * `FollowSystem` middle option stays visible. Dynamic colour is a
 * Material `Switch` since it's a simple boolean.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceCard(
    preferences: UserPreferences,
    onDarkThemeModeChange: (DarkThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCustomThemeChange: (CustomThemeOption) -> Unit,
    onCustomPaletteChange: (CustomPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var showPaletteEditor by remember { mutableStateOf(false) }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.settings_appearance_title),
        icon = Icons.Outlined.Palette,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                DarkThemeMode.entries.forEach { mode ->
                    GadgetChip(
                        selected = preferences.darkThemeMode == mode,
                        onClick = { onDarkThemeModeChange(mode) },
                        label = mode.toDisplayLabel(),
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_palette),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Live preview swatches: each palette renders its own surface +
            // primary/secondary/tertiary accents at the brightness the app
            // would actually use, so the choice is visible before it's applied.
            val systemDark = isSystemInDarkTheme()
            val previewDark = when (preferences.darkThemeMode) {
                DarkThemeMode.Light -> false
                DarkThemeMode.Dark -> true
                DarkThemeMode.FollowSystem -> systemDark
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                CustomThemeOption.entries.forEach { option ->
                    ThemeSwatch(
                        option = option,
                        selected = preferences.customTheme == option,
                        dark = previewDark,
                        // The Custom swatch previews the user's own accents;
                        // the fixed palettes derive theirs from the scheme.
                        customDots = if (option == CustomThemeOption.Custom) {
                            listOf(
                                Color(preferences.customPalette.primaryArgb),
                                Color(preferences.customPalette.secondaryArgb),
                                Color(preferences.customPalette.tertiaryArgb),
                            )
                        } else {
                            null
                        },
                        onClick = { onCustomThemeChange(option) },
                    )
                }
            }
            if (preferences.customTheme == CustomThemeOption.Custom) {
                GadgetChip(
                    selected = false,
                    onClick = { showPaletteEditor = true },
                    label = stringResource(R.string.settings_edit_custom_palette),
                )
            }
            SettingsToggleRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = if (preferences.customTheme == CustomThemeOption.Default) {
                    stringResource(R.string.settings_dynamic_color_on)
                } else {
                    stringResource(R.string.settings_dynamic_color_overridden)
                },
                checked = preferences.dynamicColor &&
                    preferences.customTheme == CustomThemeOption.Default,
                onCheckedChange = onDynamicColorChange,
                enabled = preferences.customTheme == CustomThemeOption.Default,
            )
        }
    }

    if (showPaletteEditor) {
        CustomPaletteEditorSheet(
            palette = preferences.customPalette,
            onPaletteChange = onCustomPaletteChange,
            onDismiss = { showPaletteEditor = false },
        )
    }
}

/**
 * Bottom sheet for the [CustomThemeOption.Custom] accent palette: three
 * [GadgetColorPicker]s (primary / secondary / tertiary). Changes apply live
 * through [onPaletteChange] so the whole app re-themes as the user drags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPaletteEditorSheet(
    palette: CustomPalette,
    onPaletteChange: (CustomPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.settings_custom_palette_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            PaletteAccentField(
                label = stringResource(R.string.settings_palette_primary),
                argb = palette.primaryArgb,
                onArgbChange = { onPaletteChange(palette.copy(primaryArgb = it)) },
            )
            PaletteAccentField(
                label = stringResource(R.string.settings_palette_secondary),
                argb = palette.secondaryArgb,
                onArgbChange = { onPaletteChange(palette.copy(secondaryArgb = it)) },
            )
            PaletteAccentField(
                label = stringResource(R.string.settings_palette_tertiary),
                argb = palette.tertiaryArgb,
                onArgbChange = { onPaletteChange(palette.copy(tertiaryArgb = it)) },
            )
        }
    }
}

@Composable
private fun PaletteAccentField(
    label: String,
    argb: Long,
    onArgbChange: (Long) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        GadgetColorPicker(argb = argb, onArgbChange = onArgbChange)
    }
}

/**
 * Title + subtitle column on the left, M3 [Switch] on the right.
 * Shared between Appearance + Accessibility cards.
 */
@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth(fraction = 0.75f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/**
 * One palette preview tile: a rounded surface in the palette's own
 * background colour carrying three accent dots (primary / secondary /
 * tertiary), captioned with the palette name. The selected tile gets a
 * thicker primary-coloured border and primary-tinted caption.
 *
 * [dark] resolves which brightness variant each palette renders so the
 * swatch matches what the app would paint given the current dark-theme
 * mode. [GadgetCustomTheme.Default] has no fixed palette, so it previews
 * the canonical dark/light scheme it falls back to.
 */
@Composable
private fun ThemeSwatch(
    option: CustomThemeOption,
    selected: Boolean,
    dark: Boolean,
    customDots: List<Color>?,
    onClick: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val shapes = LocalGadgetTheme.current.shapes
    val scheme = option.toGadgetCustomTheme().colorScheme(dark)
        ?: if (dark) GadgetDarkColorScheme else GadgetLightColorScheme
    val accent = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.micro),
    ) {
        Box(
            modifier = Modifier
                .size(width = ThemeSwatchDefaults.Width, height = ThemeSwatchDefaults.Height)
                .clip(shapes.medium)
                .background(scheme.surface)
                .border(
                    width = if (selected) {
                        ThemeSwatchDefaults.SelectedBorderWidth
                    } else {
                        ThemeSwatchDefaults.BorderWidth
                    },
                    color = accent,
                    shape = shapes.medium,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.small),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.micro)) {
                val dots = customDots ?: listOf(scheme.primary, scheme.secondary, scheme.tertiary)
                dots.forEach { SwatchDot(it) }
            }
        }
        Text(
            text = option.toDisplayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SwatchDot(color: Color) {
    Box(
        modifier = Modifier
            .size(ThemeSwatchDefaults.DotSize)
            .clip(CircleShape)
            .background(color),
    )
}

private object ThemeSwatchDefaults {
    val Width: Dp = 76.dp
    val Height: Dp = 52.dp
    val SelectedBorderWidth: Dp = 2.dp
    val BorderWidth: Dp = 1.dp
    val DotSize: Dp = 16.dp
}

private fun CustomThemeOption.toGadgetCustomTheme(): GadgetCustomTheme = when (this) {
    CustomThemeOption.Default -> GadgetCustomTheme.Default
    CustomThemeOption.HighContrast -> GadgetCustomTheme.HighContrast
    CustomThemeOption.AmoledTrue -> GadgetCustomTheme.AmoledTrue
    CustomThemeOption.Pastel -> GadgetCustomTheme.Pastel
    // Custom's dots come from the user palette (customDots), so the swatch base
    // just uses the canonical scheme — no fixed GadgetCustomTheme maps to it.
    CustomThemeOption.Custom -> GadgetCustomTheme.Default
}

@Composable
private fun DarkThemeMode.toDisplayLabel(): String = when (this) {
    DarkThemeMode.Light -> stringResource(R.string.settings_theme_light)
    DarkThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
    DarkThemeMode.FollowSystem -> stringResource(R.string.settings_theme_follow_system)
}

@Composable
private fun CustomThemeOption.toDisplayLabel(): String = when (this) {
    CustomThemeOption.Default -> stringResource(R.string.settings_palette_default)
    CustomThemeOption.HighContrast -> stringResource(R.string.settings_palette_high_contrast)
    CustomThemeOption.AmoledTrue -> stringResource(R.string.settings_palette_amoled)
    CustomThemeOption.Pastel -> stringResource(R.string.settings_palette_pastel)
    CustomThemeOption.Custom -> stringResource(R.string.settings_palette_custom)
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun AppearanceCardPreview() = GadgetThemedPreview {
    AppearanceCard(
        preferences = UserPreferences(customTheme = CustomThemeOption.Pastel),
        onDarkThemeModeChange = {},
        onDynamicColorChange = {},
        onCustomThemeChange = {},
        onCustomPaletteChange = {},
    )
}
