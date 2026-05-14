package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip

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
@Composable
internal fun AppearanceCard(
    preferences: UserPreferences,
    onDarkThemeModeChange: (DarkThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
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
            SettingsToggleRow(
                title = "Dynamic colour",
                subtitle = "Use your wallpaper's palette (Android 12+)",
                checked = preferences.dynamicColor,
                onCheckedChange = onDynamicColorChange,
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun DarkThemeMode.toDisplayLabel(): String = when (this) {
    DarkThemeMode.Light -> "Light"
    DarkThemeMode.Dark -> "Dark"
    DarkThemeMode.FollowSystem -> "Follow system"
}
