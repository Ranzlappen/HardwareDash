package dev.ranzlappen.gadget.feature.radios.cell

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.cellScreen() {
    composable(route = GadgetDestination.RadiosCell.route) { CellScreen() }
}
