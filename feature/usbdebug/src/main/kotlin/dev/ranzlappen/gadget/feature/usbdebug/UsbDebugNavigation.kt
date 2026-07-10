package dev.ranzlappen.gadget.feature.usbdebug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.usbDebugScreen() {
    composable(route = GadgetDestination.UsbDebug.route) { UsbDebugScreen() }
}
