package dev.ranzlappen.gadget.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire `:feature:settings` into the Gadget NavGraph.
 *
 * Call from the app's `GadgetApp { … }` builder block in
 * `MainActivity.setContent` — replacing the
 * `placeholderScreen(GadgetDestination.Settings)` line that Phase 1
 * used to stub the route:
 *
 * ```kotlin
 * GadgetApp {
 *     dashboardScreen(onNavigate = …)
 *     settingsScreen()
 * }
 * ```
 *
 * Settings sits at a top-level destination (Settings is in
 * `GadgetDestination.topLevel`), so the nav rail renders its icon
 * automatically and the route is reachable via `navigateTopLevel`.
 */
fun NavGraphBuilder.settingsScreen() {
    composable(route = GadgetDestination.Settings.route) {
        SettingsScreen()
    }
}
