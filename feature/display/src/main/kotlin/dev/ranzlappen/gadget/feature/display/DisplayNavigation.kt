package dev.ranzlappen.gadget.feature.display

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.displayScreen() {
    composable(route = GadgetDestination.Display.route) { DisplayScreen() }
}
