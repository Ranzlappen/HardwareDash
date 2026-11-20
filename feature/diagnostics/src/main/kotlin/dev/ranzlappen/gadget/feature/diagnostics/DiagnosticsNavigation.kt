package dev.ranzlappen.gadget.feature.diagnostics

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * "Device & debug" (v3 B8): this screen plus links to the ADB, USB and
 * permission-health screens, which their own modules register under
 * `diagnostics/…`. Navigation is by route — no feature-to-feature dependency.
 */
fun NavGraphBuilder.diagnosticsScreen(navController: NavHostController) {
    composable(route = GadgetDestination.Diagnostics.route) {
        DiagnosticsScreen(onOpen = { destination -> navController.navigate(destination.route) })
    }
}
