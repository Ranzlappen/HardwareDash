package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOn
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
 * Phase 1 dashboard module.
 *
 * Uses the [ModuleScreenScaffold] with a functional + disclaimer
 * slot. The dashboard is the app home — not a hardware module — so it
 * supplies no `moduleInfo` (no permissions / OS-compat / firmware
 * block). Phase 2 / Batch 1 adds the first real feature tile — Torch —
 * inside the functional slot. Future batches grow the functional slot
 * into a true adaptive grid of hardware readouts.
 *
 * [onNavigate] dispatches by route — the host (MainActivity)
 * decides whether to use `navigateTopLevel` (for top-level
 * destinations) or `navigate(...)` (for sub-routes like
 * [GadgetDestination.Torch]).
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
    )
}

@GadgetPreviewLightDark
@Composable
private fun DashboardScreenPreview() = GadgetThemedPreview {
    DashboardScreen(onNavigate = {})
}
