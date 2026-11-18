package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme

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
 * Labelled bottom navigation bar with the five [GadgetSection]s. Used on
 * Compact widths (phones in portrait).
 *
 * Insets are applied by the enclosing shell ([GadgetApp]), so the bar is
 * given zero window insets to avoid double padding above the system
 * navigation bar.
 */
@Composable
fun GadgetSectionBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val current = rememberCurrentSection(navController)
    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        GadgetSection.entries.forEach { section ->
            val selected = section == current.section
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToSection(section, current.route) },
                icon = { SectionIcon(section, selected) },
                label = { SectionLabel(section) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Labelled vertical rail with the five [GadgetSection]s. Used on Medium /
 * Expanded widths (landscape, foldables, tablets).
 */
@Composable
fun GadgetSectionRail(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val current = rememberCurrentSection(navController)
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(RailWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.tiny, Alignment.CenterVertically),
    ) {
        GadgetSection.entries.forEach { section ->
            val selected = section == current.section
            NavigationRailItem(
                selected = selected,
                onClick = { navController.navigateToSection(section, current.route) },
                icon = { SectionIcon(section, selected) },
                label = { SectionLabel(section) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Navigate to [section]'s root. Tapping the section that is already
 * selected while a tool is open pops back to that section's hub;
 * switching sections saves and restores each section's back stack.
 */
fun NavHostController.navigateToSection(section: GadgetSection, currentRoute: String?) {
    val alreadyInSection = currentRoute != null && section.owns(currentRoute)
    if (alreadyInSection) {
        if (currentRoute != section.route) {
            val popped = popBackStack(section.route, inclusive = false)
            if (!popped) navigateSectionRoot(section)
        }
        return
    }
    navigateSectionRoot(section)
}

/**
 * Open [destination] from outside the app (notification, widget, service):
 * reset to the owning section's root, then push the tool, so back leads to
 * the hub and the bottom bar highlights the right section. Section roots
 * themselves are just opened.
 */
fun NavHostController.openFromDeepLink(destination: GadgetDestination) {
    val section = GadgetSection.forRoute(destination.route)
    if (section == null) {
        navigate(destination.route) { launchSingleTop = true }
        return
    }
    navigate(section.route) {
        popUpTo(graph.startDestinationId)
        launchSingleTop = true
    }
    if (destination.route != section.route) {
        navigate(destination.route) { launchSingleTop = true }
    }
}

private fun NavHostController.navigateSectionRoot(section: GadgetSection) {
    navigate(section.route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private data class CurrentSection(val route: String?, val section: GadgetSection)

/**
 * The section owning the current back-stack entry. Routes outside every
 * section (rare — e.g. a screen registered without a destination) keep
 * the last known section highlighted instead of clearing the selection.
 */
@Composable
private fun rememberCurrentSection(navController: NavHostController): CurrentSection {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    var last by remember { mutableStateOf(GadgetSection.Home) }
    val resolved = GadgetSection.forRoute(route)
    if (resolved != null && resolved != last) {
        SideEffect { last = resolved }
    }
    return CurrentSection(route, resolved ?: last)
}

@Composable
private fun SectionIcon(section: GadgetSection, selected: Boolean) {
    Icon(
        imageVector = if (selected) section.iconFilled else section.iconOutlined,
        // The visible label (SectionLabel) names the item.
        contentDescription = null,
    )
}

@Composable
private fun SectionLabel(section: GadgetSection) {
    Text(
        text = section.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
