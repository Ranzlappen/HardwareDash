package dev.ranzlappen.gadget.feature.notification

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire `:feature:notification`'s screen route into the Gadget NavGraph.
 * Mirrors `torchScreen` / `lockScreen`.
 *
 * [onNavigateToSettings] lets the "turned off in Settings" snackbar action
 * deep-link to the Settings screen where a rooted feature's opt-in lives.
 */
fun NavGraphBuilder.notificationScreen(
    onNavigateToSettings: () -> Unit = {},
) {
    composable(route = GadgetDestination.Notification.route) {
        NotificationScreen(onNavigateToSettings = onNavigateToSettings)
    }
}
