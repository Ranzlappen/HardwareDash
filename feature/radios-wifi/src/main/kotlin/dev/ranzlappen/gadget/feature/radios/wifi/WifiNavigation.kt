package dev.ranzlappen.gadget.feature.radios.wifi

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.wifiScreen() {
    composable(route = GadgetDestination.RadiosWifi.route) { WifiScreen() }
}
