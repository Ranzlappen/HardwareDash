package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme

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
 * Centered "Coming soon" placeholder. Public so previews can render it
 * in isolation; production callers normally reach it via
 * [placeholderScreen].
 */
@Composable
fun ComingSoonScreen(
    destination: GadgetDestination,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = destination.iconOutlined,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = destination.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
