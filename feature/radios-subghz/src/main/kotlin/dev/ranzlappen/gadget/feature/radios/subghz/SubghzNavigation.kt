package dev.ranzlappen.gadget.feature.radios.subghz

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.subghzScreen() {
    composable(route = GadgetDestination.RadiosSubghz.route) { SubghzScreen() }
}
