package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Vertical icon-only navigation rail.
 *
 * Wraps a Material 3 [NavigationRail] in a [verticalScroll] so the rail
 * keeps its layout when the number of [destinations] exceeds the
 * viewport height. Items are icon-only — no text label — to match the
 * brief's "icon-only items (no text)" requirement.
 *
 * Selection is derived live from
 * [NavHostController.currentBackStackEntryAsState] so the highlight
 * stays in sync with navigation without a separate state holder.
 * Clicks route via [NavHostController.navigateTopLevel] so the back
 * stack doesn't grow indefinitely from tab switching.
 */
@Composable
fun GadgetNavRail(
    navController: NavHostController,
    destinations: List<GadgetDestination>,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val scroll = rememberScrollState()

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scroll),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigateTopLevel(destination)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                        contentDescription = destination.label,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
