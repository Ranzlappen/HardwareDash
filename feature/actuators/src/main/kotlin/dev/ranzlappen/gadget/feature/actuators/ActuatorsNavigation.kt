package dev.ranzlappen.gadget.feature.actuators

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.actuatorsScreen() {
    composable(route = GadgetDestination.Actuators.route) { ActuatorsScreen() }
}
