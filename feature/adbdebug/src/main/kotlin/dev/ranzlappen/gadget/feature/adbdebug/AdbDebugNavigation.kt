package dev.ranzlappen.gadget.feature.adbdebug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.adbDebugScreen() {
    composable(route = GadgetDestination.AdbDebug.route) {
        AdbDebugScreen()
    }
}
