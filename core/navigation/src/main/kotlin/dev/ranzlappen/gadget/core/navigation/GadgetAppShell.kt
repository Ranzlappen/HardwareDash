package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.glassSurface

/**
 * Top-level Gadget app shell.
 *
 * A [Scaffold] with:
 *   * A glassmorphic bottom [NavigationBar] showing the five top-level
 *     destinations from [GadgetDestination.topLevel].
 *   * A [GadgetNavHost] body. Feature modules contribute their screens
 *     via the [builder] block, exactly like vanilla [GadgetNavHost].
 *
 * The active top-level destination is derived live from
 * [NavHostController.currentBackStackEntryAsState], so the bottom
 * bar's highlight stays in sync with navigation without a separate
 * state holder.
 *
 * Typical usage from `:app`'s `MainActivity`:
 *
 * ```kotlin
 * setContent {
 *     GadgetTheme {
 *         GadgetAppShell {
 *             dashboardScreen(onNavigate = { /* … */ })
 *             // sensorsScreen(...) — lands in a later Phase-1 batch
 *             placeholderScreen(GadgetDestination.Sensors)
 *             placeholderScreen(GadgetDestination.Actuators)
 *             placeholderScreen(GadgetDestination.Automation)
 *             placeholderScreen(GadgetDestination.Settings)
 *         }
 *     }
 * }
 * ```
 *
 * Until every top-level destination has a real feature module, fill
 * the gaps with [placeholderScreen]; tapping a nav-bar item that
 * isn't registered would otherwise crash NavHost.
 */
@Composable
fun GadgetAppShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: GadgetDestination = GadgetDestination.Dashboard,
    builder: NavGraphBuilder.() -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            GadgetBottomBar(
                currentRoute = currentRoute,
                onSelect = { destination -> navController.navigateTopLevel(destination) },
            )
        },
    ) { innerPadding ->
        GadgetNavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = startDestination,
            builder = builder,
        )
    }
}

/**
 * Register a [GadgetDestination] with a "coming soon" placeholder
 * screen. Use for top-level routes whose real feature module hasn't
 * shipped yet so [GadgetAppShell]'s bottom nav has somewhere to land.
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

// ─── Bottom navigation bar ──────────────────────────────────────────

/**
 * Glassmorphic bottom navigation bar.
 *
 * Wraps a transparent [NavigationBar] in [Modifier.glassSurface] so
 * the bar reads as a frosted layer over the screen content scrolled
 * beneath it. `cornerRadius = 0.dp` keeps the bar flush to the bottom
 * edge — rounding the bottom corners would create a thin gap of
 * background that looks broken on phones.
 */
@Composable
private fun GadgetBottomBar(
    currentRoute: String?,
    onSelect: (GadgetDestination) -> Unit,
) {
    NavigationBar(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.glassSurface(
            cornerRadius = 0.dp,
            intensity = GlassIntensity.Subtle,
            showBorder = false,
        ),
    ) {
        GadgetDestination.topLevel.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
