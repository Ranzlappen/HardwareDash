package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * App home screen.
 *
 * The [functional] slot holds the primary readout / navigation tiles. On wide
 * screens (tablet or open foldable) [ModuleScreenScaffold] shows a
 * [secondaryPane] alongside or below (Tabletop posture) the primary content —
 * this pane holds quick-access shortcuts to every migrated feature.
 *
 * [onNavigate] dispatches by route; the host decides whether to use
 * `navigateTopLevel` (top-level destinations) or `navigate(...)` (sub-routes).
 */
@Composable
fun DashboardScreen(
    onNavigate: (GadgetDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = "Dashboard",
        modifier = modifier,
        functional = {
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Live readouts",
            ) {
                Text(
                    text = "Phase 1 placeholder — real readouts land later in Phase 2.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Torch",
                icon = Icons.Outlined.FlashlightOn,
                onClick = { onNavigate(GadgetDestination.Torch) },
            ) {
                Text(
                    text = "Toggle the device flashlight, view strobe controls.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        disclaimer = {
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Disclaimer",
            ) {
                Text(
                    text = "Sample placeholder content. Do not rely on these values.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        // On wide / foldable screens: feature shortcuts appear alongside or below
        // the primary content depending on device posture (side-by-side on a
        // tablet/open foldable; stacked in the bottom half on Tabletop).
        secondaryPane = {
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Quick access",
            ) {
                Text(
                    text = "Jump to a feature.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Vibration",
                icon = Icons.Outlined.Vibration,
                onClick = { onNavigate(GadgetDestination.Vibration) },
            ) {
                Text(
                    text = "Vibration patterns and motor controls.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Sensors",
                icon = Icons.Outlined.Sensors,
                onClick = { onNavigate(GadgetDestination.Sensors) },
            ) {
                Text(
                    text = "Proximity, light, and motion sensors.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Automation",
                icon = Icons.Outlined.AutoMode,
                onClick = { onNavigate(GadgetDestination.Automation) },
            ) {
                Text(
                    text = "Cross-feature automation rules.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@GadgetPreviewLightDark
@Composable
private fun DashboardScreenPreview() = GadgetThemedPreview {
    DashboardScreen(onNavigate = {})
}
