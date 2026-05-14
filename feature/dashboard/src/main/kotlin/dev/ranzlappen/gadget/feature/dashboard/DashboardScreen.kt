package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
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
 * First consumer of [ModuleScreenScaffold]. Demonstrates the
 * common 3-section pattern (functional / permissions / disclaimer)
 * with placeholder content only — no real readouts, no permission
 * wiring, no graphs. Phase 2 fills the [functional] slot with the
 * live hardware grid; Phase 1.5 adds the permissions table.
 *
 * The [onNavigate] callback is kept on the public signature as the
 * seam Phase 2 will reuse when tiles deep-link into sensor detail
 * routes. Unused at this phase.
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
                    text = "Phase 1 placeholder — real readouts land in Phase 2.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        permissions = {
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Permissions",
            ) {
                Text(
                    text = "No permissions required for this module yet.",
                    style = MaterialTheme.typography.bodySmall,
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
