package dev.ranzlappen.gadget.feature.sensors

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire :feature:sensors into the Gadget NavGraph. Call from the
 * `GadgetApp { … }` builder in :app.
 *
 * Registers at [GadgetDestination.Sensors]'s route (the torch pattern) so
 * the rail's `navigateTopLevel` and this composable can never drift apart —
 * a rename of either is a compile-time event, not a dead rail item.
 */
fun NavGraphBuilder.sensorsScreen() {
    composable(route = GadgetDestination.Sensors.route) {
        SensorsScreen()
    }
}
