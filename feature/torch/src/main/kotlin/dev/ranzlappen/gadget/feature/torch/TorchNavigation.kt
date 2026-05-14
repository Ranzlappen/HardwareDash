package dev.ranzlappen.gadget.feature.torch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire `:feature:torch`'s screen route into the Gadget NavGraph.
 *
 * Call from the app's `GadgetApp { … }` builder block:
 *
 * ```kotlin
 * GadgetApp {
 *     dashboardScreen(onNavigate = …)
 *     settingsScreen()
 *     torchScreen()
 * }
 * ```
 *
 * Torch is **not** in [GadgetDestination.topLevel] — it doesn't
 * appear in the nav rail. Reach it via:
 * - Dashboard tile tap (Phase 2 / Batch 1 wires this).
 * - QS tile (doesn't go through this route; toggles controller
 *   directly).
 * - Home-screen widget (doesn't go through this route either).
 */
fun NavGraphBuilder.torchScreen() {
    composable(route = GadgetDestination.Torch.route) {
        TorchScreen()
    }
}
