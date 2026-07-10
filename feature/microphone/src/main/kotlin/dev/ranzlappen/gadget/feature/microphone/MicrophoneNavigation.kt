package dev.ranzlappen.gadget.feature.microphone

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

fun NavGraphBuilder.microphoneScreen() {
    composable(route = GadgetDestination.Microphone.route) { MicrophoneScreen() }
}
