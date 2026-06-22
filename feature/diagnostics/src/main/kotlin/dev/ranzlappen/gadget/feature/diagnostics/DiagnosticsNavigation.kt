package dev.ranzlappen.gadget.feature.diagnostics

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.diagnosticsScreen() {
    composable(route = GadgetDestination.Diagnostics.route) { DiagnosticsScreen() }
}
