package dev.ranzlappen.gadget.feature.files

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/** Registers the Files tool (v3 B12) under [GadgetDestination.Files]. */
fun NavGraphBuilder.filesScreen() {
    composable(route = GadgetDestination.Files.route) {
        FilesScreen()
    }
}
