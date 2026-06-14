package dev.ranzlappen.gadget.feature.radios.ir

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.irScreen() {
    composable(route = GadgetDestination.RadiosIr.route) { IrScreen() }
}
