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
 * Torch is a module entry in [GadgetDestination.modules], so the nav
 * rail renders its icon in the scrollable module region and the route
 * is reachable via `navigateTopLevel`. Also reachable via:
 * - Dashboard tile tap.
 * - QS tile (doesn't go through this route; toggles controller
 *   directly).
 * - Home-screen widget (doesn't go through this route either).
 */
fun NavGraphBuilder.torchScreen() {
    composable(route = GadgetDestination.Torch.route) {
        TorchScreen()
    }
}
