package dev.ranzlappen.gadget.feature.logbook

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Registers the Logbook screen route into the app nav graph. Called from
 * `MainActivity`'s `GadgetApp { … }` builder; the rail entry already exists
 * in [GadgetDestination.Logbook].
 */
fun NavGraphBuilder.logbookScreen() {
    composable(route = GadgetDestination.Logbook.route) {
        LogbookScreen()
    }
}
