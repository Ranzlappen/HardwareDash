package dev.ranzlappen.gadget.feature.battery

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.batteryScreen() {
    composable(route = GadgetDestination.Battery.route) {
        BatteryScreen()
    }
}
