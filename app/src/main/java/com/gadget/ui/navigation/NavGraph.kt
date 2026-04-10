package com.gadget.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.gadget.localization.S
import com.gadget.ui.dashboard.DashboardScreen
import com.gadget.ui.hubs.ToolsHubScreen
import com.gadget.ui.hubs.MonitorHubScreen
import com.gadget.ui.hubs.MoreHubScreen
import com.gadget.ui.logbook.LogbookScreen

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    // ── Top-level tabs (bottom nav) ──
    const val DASHBOARD    = "dashboard"
    const val TOOLS        = "tools"
    const val MONITOR      = "monitor"
    const val LOGBOOK      = "logbook"
    const val MORE         = "more"

    // ── Hub landing grids ──
    const val TOOLS_GRID   = "tools_grid"
    const val MONITOR_GRID = "monitor_grid"
    const val MORE_GRID    = "more_grid"

    // ── Tools sub-screens ──
    const val TORCH        = "torch"
    const val CAMERA       = "camera"
    const val VIBRATION    = "vibration"
    const val MIC          = "mic"

    // ── Monitor sub-screens ──
    const val SENSORS      = "sensors"
    const val BATTERY      = "battery"
    const val RADIOS       = "radios"

    // ── More sub-screens ──
    const val LOCKSCREEN   = "lockscreen"
    const val LINK         = "link"
    const val FILE_META    = "file_meta"
    const val SETTINGS     = "settings"
    const val BUG          = "bug"
}

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD,  Icons.Filled.Home,        Icons.Outlined.Home),
    BottomNavItem(Routes.TOOLS,      Icons.Filled.Build,       Icons.Outlined.Build),
    BottomNavItem(Routes.MONITOR,    Icons.Filled.MonitorHeart, Icons.Outlined.MonitorHeart),
    BottomNavItem(Routes.LOGBOOK,    Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomNavItem(Routes.MORE,       Icons.Filled.Apps,        Icons.Outlined.Apps),
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Resolve localized labels inside composable scope
    val nav = S.nav
    val navLabels = mapOf(
        Routes.DASHBOARD to nav.dashboard,
        Routes.TOOLS     to nav.tools,
        Routes.MONITOR   to nav.monitor,
        Routes.LOGBOOK   to nav.logbook,
        Routes.MORE      to nav.more,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    val label = navLabels[item.route] ?: item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = label,
                            )
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(Routes.TOOLS)   { ToolsHubScreen() }
            composable(Routes.MONITOR) { MonitorHubScreen() }
            composable(Routes.LOGBOOK) { LogbookScreen() }
            composable(Routes.MORE)    { MoreHubScreen() }
        }
    }
}
