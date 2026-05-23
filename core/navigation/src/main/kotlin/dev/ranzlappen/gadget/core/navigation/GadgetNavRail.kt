package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Fixed-width design token for the rail.
 *
 * Matches the Material 3 [androidx.compose.material3.NavigationRail]
 * default width (80 dp). Declared as a file-scoped constant — one of
 * the documented exceptions to the "no raw dp literals" rule — because
 * it's a single fixed structural dimension, not a themeable spacing
 * token.
 */
private val RailWidth: Dp = 80.dp

/**
 * Vertical navigation rail with pinned anchors and a scrollable middle.
 *
 * Layout (top → bottom):
 *  - [topAnchors] pinned at the top (typically Dashboard),
 *  - [modules] in a weighted, [verticalScroll]-backed middle region so
 *    the rail keeps Dashboard + Settings reachable no matter how many
 *    feature modules are registered (the module list grows over time),
 *  - [bottomAnchors] pinned at the bottom (typically Settings).
 *
 * Built as a plain [Column] of [NavigationRailItem]s rather than an M3
 * [androidx.compose.material3.NavigationRail] because the rail needs a
 * *pinned-top + scrollable-middle + pinned-bottom* shape — `NavigationRail`
 * exposes only a top header slot and doesn't reliably scroll when items
 * overflow the viewport.
 *
 * Selection is derived live from
 * [NavHostController.currentBackStackEntryAsState] so the highlight
 * stays in sync without a separate state holder. Clicks route via
 * [NavHostController.navigateTopLevel] so the back stack doesn't grow
 * indefinitely from tab switching.
 */
@Composable
fun GadgetNavRail(
    navController: NavHostController,
    topAnchors: List<GadgetDestination>,
    modules: List<GadgetDestination>,
    bottomAnchors: List<GadgetDestination>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val scroll = rememberScrollState()

    val onSelect: (GadgetDestination) -> Unit = { destination ->
        if (currentRoute != destination.route) navController.navigateTopLevel(destination)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(RailWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        topAnchors.forEach { destination ->
            RailItem(destination, currentRoute == destination.route, showLabels, onSelect)
        }
        // Scrollable module region — takes the space between the
        // pinned anchors and scrolls internally when the module list
        // overflows the available height.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            modules.forEach { destination ->
                RailItem(destination, currentRoute == destination.route, showLabels, onSelect)
            }
        }
        bottomAnchors.forEach { destination ->
            RailItem(destination, currentRoute == destination.route, showLabels, onSelect)
        }
    }
}

@Composable
private fun RailItem(
    destination: GadgetDestination,
    selected: Boolean,
    showLabels: Boolean,
    onSelect: (GadgetDestination) -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = { onSelect(destination) },
        icon = {
            Icon(
                imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                contentDescription = destination.label,
            )
        },
        label = if (showLabels) {
            { Text(text = destination.label) }
        } else null,
        alwaysShowLabel = showLabels,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
