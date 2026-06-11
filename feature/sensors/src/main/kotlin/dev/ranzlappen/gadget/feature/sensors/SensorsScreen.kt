package dev.ranzlappen.gadget.feature.sensors

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Hilt entry point for the Sensors feature screen. Resolves the ViewModel
 * and renders the stateless [SensorsScreenContent] — keeping the content
 * (and its previews/tests) Hilt-free, per the module blueprint.
 */
@Composable
fun SensorsScreen(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") viewModel: SensorsViewModel = hiltViewModel(),
) {
    SensorsScreenContent(modifier = modifier)
}

/**
 * Stateless Sensors screen content. Build the real functional slot here
 * (controls, monitoring, ModuleInfo) following the torch/vibration reference.
 */
@Composable
internal fun SensorsScreenContent(modifier: Modifier = Modifier) {
    ModuleScreenScaffold(
        title = "Sensors",
        modifier = modifier,
        functional = {
            GadgetEmptyState(
                title = "Sensors coming soon",
                subtitle = "Scaffolded by scripts/new-feature.sh — build the real UI here.",
            )
        },
    )
}

@GadgetPreviewLightDark
@Composable
private fun SensorsScreenPreview() = GadgetThemedPreview { SensorsScreenContent() }
