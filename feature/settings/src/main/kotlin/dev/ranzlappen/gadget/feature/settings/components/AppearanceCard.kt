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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.datastore.CustomThemeOption
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetCustomTheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetDarkColorScheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetLightColorScheme
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.theme.colorScheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

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
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = "Appearance",
        icon = Icons.Outlined.Palette,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = "Theme",
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
                text = "Palette",
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
                        onClick = { onCustomThemeChange(option) },
                    )
                }
            }
            SettingsToggleRow(
                title = "Dynamic colour",
                subtitle = if (preferences.customTheme == CustomThemeOption.Default) {
                    "Use your wallpaper's palette (Android 12+)"
                } else {
                    "Overridden by the selected palette"
                },
                checked = preferences.dynamicColor &&
                    preferences.customTheme == CustomThemeOption.Default,
                onCheckedChange = onDynamicColorChange,
                enabled = preferences.customTheme == CustomThemeOption.Default,
            )
        }
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
                .size(width = 76.dp, height = 52.dp)
                .clip(shapes.medium)
                .background(scheme.surface)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = accent,
                    shape = shapes.medium,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.small),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.micro)) {
                SwatchDot(scheme.primary)
                SwatchDot(scheme.secondary)
                SwatchDot(scheme.tertiary)
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
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
    )
}

private fun CustomThemeOption.toGadgetCustomTheme(): GadgetCustomTheme = when (this) {
    CustomThemeOption.Default -> GadgetCustomTheme.Default
    CustomThemeOption.HighContrast -> GadgetCustomTheme.HighContrast
    CustomThemeOption.AmoledTrue -> GadgetCustomTheme.AmoledTrue
    CustomThemeOption.Pastel -> GadgetCustomTheme.Pastel
}

private fun DarkThemeMode.toDisplayLabel(): String = when (this) {
    DarkThemeMode.Light -> "Light"
    DarkThemeMode.Dark -> "Dark"
    DarkThemeMode.FollowSystem -> "Follow system"
}

private fun CustomThemeOption.toDisplayLabel(): String = when (this) {
    CustomThemeOption.Default -> "Default"
    CustomThemeOption.HighContrast -> "High contrast"
    CustomThemeOption.AmoledTrue -> "AMOLED"
    CustomThemeOption.Pastel -> "Pastel"
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
    )
}
