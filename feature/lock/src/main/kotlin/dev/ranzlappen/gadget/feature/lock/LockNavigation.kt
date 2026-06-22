package dev.ranzlappen.gadget.feature.lock

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.lockScreen() {
    composable(route = GadgetDestination.Lock.route) { LockScreen() }
}
