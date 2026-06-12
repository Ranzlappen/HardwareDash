package dev.ranzlappen.gadget.feature.automation.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire :feature:automation-ui into the Gadget NavGraph. Call from the
 * `GadgetApp { … }` builder in :app.
 *
 * Registers at [GadgetDestination.Automation]'s route (the torch pattern)
 * so the rail's `navigateTopLevel` and this composable can never drift
 * apart — a rename of either is a compile-time event, not a dead rail item.
 */
fun NavGraphBuilder.automationScreen() {
    composable(route = GadgetDestination.Automation.route) {
        AutomationScreen()
    }
}
