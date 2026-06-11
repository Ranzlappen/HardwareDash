package dev.ranzlappen.gadget.feature.sensors

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Route string for the Sensors destination. Kept module-local so this module
 * compiles standalone; once you add `GadgetDestination.Sensors` (see the
 * script's "manual steps"), switch this to `GadgetDestination.Sensors.route`.
 */
const val SENSORS_ROUTE = "sensors"

/**
 * Wire :feature:sensors into the Gadget NavGraph. Call from the
 * `GadgetApp { … }` builder in :app.
 */
fun NavGraphBuilder.sensorsScreen() {
    composable(route = SENSORS_ROUTE) {
        SensorsScreen()
    }
}
