package dev.ranzlappen.gadget.feature.audio

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * The Audio screen. Its rooted-tools section links to the advanced
 * microphone tools ([GadgetDestination.Microphone], registered by
 * `:feature:microphone`) — navigated by route, so there is no
 * feature-to-feature dependency.
 */
fun NavGraphBuilder.audioScreen(navController: NavHostController) {
    composable(route = GadgetDestination.Audio.route) {
        AudioScreen(onOpenMicrophoneTools = { navController.navigate(GadgetDestination.Microphone.route) })
    }
}
