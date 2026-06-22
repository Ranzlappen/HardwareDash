package dev.ranzlappen.gadget.feature.manual

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.manualScreen() {
    composable(route = GadgetDestination.Manual.route) { ManualScreen() }
}
