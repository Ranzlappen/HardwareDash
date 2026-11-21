package dev.ranzlappen.gadget.feature.sensors

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire the sensor explorer into the Gadget NavGraph (call from the
 * `GadgetApp { … }` builder in :app): the list at
 * [GadgetDestination.Sensors]'s route and one detail screen per sensor at
 * `sensors/detail/{index}`. The detail route starts with the list's route,
 * so the shell keeps the Sensors section selected and shows the up arrow.
 */
fun NavGraphBuilder.sensorsScreen(navController: NavHostController) {
    composable(route = GadgetDestination.Sensors.route) {
        SensorsScreen(onOpenSensor = { index -> navController.navigate(sensorDetailRoute(index)) })
    }
    composable(
        route = DETAIL_ROUTE,
        arguments = listOf(navArgument(SensorDetailViewModel.ARG_SENSOR_INDEX) { type = NavType.IntType }),
    ) {
        SensorDetailScreen()
    }
}

private val DETAIL_ROUTE = "${GadgetDestination.Sensors.route}/detail/{${SensorDetailViewModel.ARG_SENSOR_INDEX}}"

internal fun sensorDetailRoute(index: Int): String = "${GadgetDestination.Sensors.route}/detail/$index"
