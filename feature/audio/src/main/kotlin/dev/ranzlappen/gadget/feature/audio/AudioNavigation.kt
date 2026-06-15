package dev.ranzlappen.gadget.feature.audio

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.audioScreen() {
    composable(route = GadgetDestination.Audio.route) { AudioScreen() }
}
