package dev.ranzlappen.gadget.feature.ambient

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.ambientScreen() {
    composable(route = GadgetDestination.Ambient.route) { AmbientScreen() }
}
