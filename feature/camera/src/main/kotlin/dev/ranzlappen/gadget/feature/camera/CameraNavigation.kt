package dev.ranzlappen.gadget.feature.camera

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.cameraScreen() {
    composable(route = GadgetDestination.Camera.route) { CameraScreen() }
}
