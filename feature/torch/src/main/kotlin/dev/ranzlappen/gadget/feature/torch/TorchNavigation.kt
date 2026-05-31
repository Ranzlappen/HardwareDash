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
 *     settingsScreen(rootFeatureToggles = { RootedFeatureTogglesCard() })
 *     torchScreen(onNavigateToSettings = { navController.navigateTopLevel(Settings) })
 * }
 * ```
 *
 * [onNavigateToSettings] lets the rooted Root-tools card deep-link to the
 * Settings screen where a tool's opt-in lives (the "Enable" action on the
 * "turned off in settings" snackbar). The default no-op keeps the route
 * usable without navigation wiring (previews / tests).
 *
 * Torch is a module entry in [GadgetDestination.modules], so the nav
 * rail renders its icon in the scrollable module region and the route
 * is reachable via `navigateTopLevel`. Also reachable via:
 * - Dashboard tile tap.
 * - QS tile (doesn't go through this route; toggles controller
 *   directly).
 * - Home-screen widget (doesn't go through this route either).
 */
fun NavGraphBuilder.torchScreen(
    onNavigateToSettings: () -> Unit = {},
) {
    composable(route = GadgetDestination.Torch.route) {
        TorchScreen(onNavigateToSettings = onNavigateToSettings)
    }
}
