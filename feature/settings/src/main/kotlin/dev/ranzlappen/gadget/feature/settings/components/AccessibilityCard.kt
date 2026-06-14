package dev.ranzlappen.gadget.feature.settings.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip

/**
 * Accessibility card — controls:
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
 *   - **Floating torch button** (boolean). Starts
 *     `TorchOverlayService` which displays a draggable
 *     `SYSTEM_ALERT_WINDOW` button to toggle the torch from
 *     any screen. Requires the overlay permission; this card
 *     re-checks the permission on each resume so the toggle
 *     unlocks immediately after the user grants it in system
 *     settings.
 */
@Composable
internal fun AccessibilityCard(
    preferences: UserPreferences,
    onReducedMotionOverrideChange: (TriStatePreference) -> Unit,
    onReducedTransparencyChange: (Boolean) -> Unit,
    onLargeTextOverrideChange: (Boolean) -> Unit,
    onFloatingTorchButtonEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check overlay permission when the screen resumes (user may have
    // just granted it in system settings and navigated back).
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            SettingsToggleRow(
                title = "Floating torch button",
                subtitle = if (canDrawOverlays) {
                    "Show a draggable button over other apps to toggle the torch"
                } else {
                    "Requires overlay permission — tap to grant in system settings"
                },
                checked = preferences.floatingTorchButtonEnabled && canDrawOverlays,
                onCheckedChange = { enabled ->
                    if (enabled && !canDrawOverlays) {
                        // Redirect to the system overlay-permission screen; the
                        // DisposableEffect above re-checks on resume.
                        ctx.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}"),
                            ),
                        )
                    } else {
                        onFloatingTorchButtonEnabledChange(enabled)
                    }
                },
            )
        }
    }
}

private fun TriStatePreference.toDisplayLabel(): String = when (this) {
    TriStatePreference.On -> "On"
    TriStatePreference.Off -> "Off"
    TriStatePreference.FollowSystem -> "Follow system"
}
