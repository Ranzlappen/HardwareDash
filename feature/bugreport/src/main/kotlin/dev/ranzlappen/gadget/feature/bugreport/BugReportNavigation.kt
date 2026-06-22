package dev.ranzlappen.gadget.feature.bugreport

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.bugReportScreen() {
    composable(route = GadgetDestination.BugReport.route) { BugReportScreen() }
}
