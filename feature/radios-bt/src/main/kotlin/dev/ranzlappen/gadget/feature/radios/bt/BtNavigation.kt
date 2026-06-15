package dev.ranzlappen.gadget.feature.radios.bt

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.btScreen() {
    composable(route = GadgetDestination.RadiosBt.route) { BtScreen() }
}
