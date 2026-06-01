package dev.ranzlappen.gadget.feature.vibration

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire `:feature:vibration`'s screen route into the Gadget NavGraph.
 *
 * Call from the app's `GadgetApp { … }` builder block:
 *
 * ```kotlin
 * GadgetApp {
 *     dashboardScreen(onNavigate = …)
 *     vibrationScreen(onNavigateToSettings = { navController.navigateTopLevel(Settings) })
 * }
 * ```
 *
 * [onNavigateToSettings] lets the rooted Root-tools card deep-link to the
 * Settings screen where a tool's opt-in lives. Vibration is a module entry in
 * [GadgetDestination.modules], so the nav rail renders its icon in the
 * scrollable module region.
 */
fun NavGraphBuilder.vibrationScreen(
    onNavigateToSettings: () -> Unit = {},
) {
    composable(route = GadgetDestination.Vibration.route) {
        VibrationScreen(onNavigateToSettings = onNavigateToSettings)
    }
}
