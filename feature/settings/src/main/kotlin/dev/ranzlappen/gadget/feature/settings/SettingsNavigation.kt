package dev.ranzlappen.gadget.feature.settings

import androidx.compose.runtime.Composable
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
 *     settingsScreen(rootFeatureToggles = { RootedFeatureTogglesCard() })
 * }
 * ```
 *
 * [rootFeatureToggles] is the slot `:app` fills with its
 * `RootedFeatureTogglesCard` (the rooted-feature opt-in switches +
 * safety-mode master switch). It lives in `:app/src/main` because it
 * depends on the legacy `RootFeaturesEntryPoint` + 22 controllers that a
 * leaf feature module can't reference; the default empty slot keeps the
 * route Hilt-free for any caller that doesn't supply it.
 *
 * Settings is pinned to the bottom of the rail (it's in
 * `GadgetDestination.pinnedBottom`), so the nav rail renders its icon
 * automatically and the route is reachable via `navigateTopLevel`.
 */
fun NavGraphBuilder.settingsScreen(
    backupSection: @Composable () -> Unit = {},
    rootFeatureToggles: @Composable () -> Unit = {},
) {
    composable(route = GadgetDestination.Settings.route) {
        SettingsScreen(backupSection = backupSection, rootFeatureToggles = rootFeatureToggles)
    }
}
