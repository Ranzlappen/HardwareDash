package dev.ranzlappen.gadget.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire :feature:dashboard into the Gadget [androidx.navigation.NavGraph].
 *
 * Call from the app's `GadgetAppShell { … }` builder block:
 *
 * ```kotlin
 * GadgetAppShell {
 *     dashboardScreen(onNavigate = { destination ->
 *         navController.navigateTopLevel(destination)
 *     })
 *     // … other features
 * }
 * ```
 *
 * The single [onNavigate] callback handles every navigation request
 * the dashboard can produce. The caller chooses whether to use
 * `NavController.navigateTopLevel` (back-stack-trimming) or a plain
 * `navigate(...)` — the dashboard doesn't care.
 */
fun NavGraphBuilder.dashboardScreen(
    onNavigate: (GadgetDestination) -> Unit,
) {
    composable(route = GadgetDestination.Dashboard.route) {
        DashboardScreen(onNavigate = onNavigate)
    }
}
