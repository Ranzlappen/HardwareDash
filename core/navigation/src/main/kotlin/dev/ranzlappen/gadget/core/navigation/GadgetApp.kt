package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold

/**
 * Top-level Gadget app shell.
 *
 * Side-rail layout: [GadgetTheme] wraps a [Row] containing the icon-only
 * [GadgetNavRail] on the left and a [GadgetNavHost] body that takes the
 * remaining width. Feature modules contribute screens via the [builder]
 * block, exactly like vanilla [GadgetNavHost].
 *
 * Typical usage from `:app`'s `MainActivity`:
 *
 * ```kotlin
 * setContent {
 *     when (val resolved = outcome) {
 *         LaunchGateOutcome.Allowed -> GadgetApp {
 *             dashboardScreen(onNavigate = { /* … */ })
 *             placeholderScreen(GadgetDestination.Sensors)
 *             // …
 *         }
 *         // …
 *     }
 * }
 * ```
 *
 * Until every top-level destination has a real feature module, fill
 * the gaps with [placeholderScreen]; tapping a rail item that isn't
 * registered would otherwise crash NavHost.
 */
@Composable
fun GadgetApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: GadgetDestination = GadgetDestination.Dashboard,
    builder: NavGraphBuilder.() -> Unit,
) {
    GadgetTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                GadgetNavRail(
                    navController = navController,
                    destinations = GadgetDestination.topLevel,
                )
                GadgetNavHost(
                    modifier = Modifier.weight(1f),
                    navController = navController,
                    startDestination = startDestination,
                    builder = builder,
                )
            }
        }
    }
}

/**
 * Register a [GadgetDestination] with a "coming soon" placeholder
 * screen. Use for top-level routes whose real feature module hasn't
 * shipped yet so [GadgetApp]'s rail has somewhere to land.
 */
fun NavGraphBuilder.placeholderScreen(destination: GadgetDestination) {
    composable(route = destination.route) {
        ComingSoonScreen(destination)
    }
}

/**
 * "Coming soon" placeholder rendered via [ModuleScreenScaffold].
 *
 * Each top-level destination gets its own short, module-appropriate
 * blurb in the scaffold's `functional` slot; the `permissions` and
 * `disclaimer` slots are deliberately left empty in Phase 1 and will
 * be populated in Phase 1.5 as each feature module lands.
 *
 * Public so previews can render it in isolation; production callers
 * normally reach it via [placeholderScreen].
 */
@Composable
fun ComingSoonScreen(
    destination: GadgetDestination,
    modifier: Modifier = Modifier,
) {
    val message = when (destination) {
        GadgetDestination.Dashboard -> "Dashboard overview coming in Phase 2"
        GadgetDestination.Sensors -> "Sensor overview coming in Phase 2"
        GadgetDestination.Actuators -> "Actuator controls coming in Phase 2"
        GadgetDestination.Automation -> "Automation rules coming in Phase 2"
        GadgetDestination.Settings -> "Settings coming in Phase 2"
    }
    ModuleScreenScaffold(
        title = destination.label,
        modifier = modifier,
        functional = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
