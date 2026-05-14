package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip

/**
 * Accessibility card — three controls:
 *
 *   - **Reduce motion** (tri-state). Override the system's
 *     animator-duration-scale signal that drives
 *     `LocalReducedMotion`. `FollowSystem` defers to the OS
 *     setting; explicit `On` / `Off` forces the override.
 *   - **Reduce transparency** (boolean). Drives
 *     `LocalReducedTransparency` — glassy surfaces swap to the
 *     highest-opacity Subtle preset when on.
 *   - **Increase text size** (boolean). Reserved for a future
 *     font-scale override; currently a no-op visually but the
 *     preference persists so the toggle isn't dead UX.
 */
@Composable
internal fun AccessibilityCard(
    preferences: UserPreferences,
    onReducedMotionOverrideChange: (TriStatePreference) -> Unit,
    onReducedTransparencyChange: (Boolean) -> Unit,
    onLargeTextOverrideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = "Accessibility",
        icon = Icons.Outlined.Accessibility,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = "Reduce motion",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                TriStatePreference.entries.forEach { value ->
                    GadgetChip(
                        selected = preferences.reducedMotionOverride == value,
                        onClick = { onReducedMotionOverrideChange(value) },
                        label = value.toDisplayLabel(),
                    )
                }
            }
            SettingsToggleRow(
                title = "Reduce transparency",
                subtitle = "Swap glassy surfaces to a higher-opacity preset",
                checked = preferences.reducedTransparency,
                onCheckedChange = onReducedTransparencyChange,
            )
            SettingsToggleRow(
                title = "Increase text size",
                subtitle = "Reserved — wires to a font-scale override in a later batch",
                checked = preferences.largeTextOverride,
                onCheckedChange = onLargeTextOverrideChange,
            )
        }
    }
}

private fun TriStatePreference.toDisplayLabel(): String = when (this) {
    TriStatePreference.On -> "On"
    TriStatePreference.Off -> "Off"
    TriStatePreference.FollowSystem -> "Follow system"
}
