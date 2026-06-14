package dev.ranzlappen.gadget.feature.gps

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.gpsScreen() {
    composable(route = GadgetDestination.Gps.route) {
        GpsScreen()
    }
}
