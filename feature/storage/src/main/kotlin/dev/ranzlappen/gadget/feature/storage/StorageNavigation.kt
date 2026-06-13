package dev.ranzlappen.gadget.feature.storage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.storageScreen() {
    composable(route = GadgetDestination.Storage.route) {
        StorageScreen()
    }
}
