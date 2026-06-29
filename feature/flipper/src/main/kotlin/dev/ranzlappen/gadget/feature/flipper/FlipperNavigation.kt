package dev.ranzlappen.gadget.feature.flipper

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.flipperScreen() {
    composable(route = GadgetDestination.Flipper.route) { FlipperScreen() }
}
