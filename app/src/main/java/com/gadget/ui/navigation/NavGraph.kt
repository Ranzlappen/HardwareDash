package com.gadget.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.gadget.localization.S
import android.content.Context
import com.gadget.ui.dashboard.DashboardScreen
import com.gadget.ui.hubs.ToolsHubScreen
import com.gadget.ui.hubs.MonitorHubScreen
import com.gadget.ui.hubs.MoreHubScreen
import com.gadget.ui.logbook.LogbookScreen
import com.gadget.ui.charts.MetricHistoryScreen
import com.gadget.ui.onboarding.OnboardingScreen
import com.gadget.ui.search.SearchScreen
import com.gadget.ui.theme.LocalAccessibilityPreferences

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

    // ── Onboarding ──
    const val ONBOARDING   = "onboarding"

    // ── More sub-screens ──
    const val LOCKSCREEN   = "lockscreen"
    const val LINK         = "link"
    const val FILE_META    = "file_meta"
    const val SETTINGS     = "settings"
    const val BUG          = "bug"
    const val MANUAL       = "manual"

    // ── Charts ──
    const val METRIC_HISTORY = "metric_history/{metricKey}"
    fun metricHistory(metricKey: String) = "metric_history/$metricKey"

    // ── Global Search ──
    const val SEARCH       = "search"
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasSeenOnboarding = remember {
        context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
            .getBoolean("has_seen_onboarding", false)
    }
    val startDest = if (hasSeenOnboarding) Routes.DASHBOARD else Routes.ONBOARDING

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val accessibilityPrefs = LocalAccessibilityPreferences.current
    val contentFocusRequester = remember { FocusRequester() }

    // Resolve localized labels inside composable scope
    val nav = S.nav
    val a11y = S.accessibility
    val navLabels = mapOf(
        Routes.DASHBOARD to nav.dashboard,
        Routes.TOOLS     to nav.tools,
        Routes.MONITOR   to nav.monitor,
        Routes.LOGBOOK   to nav.logbook,
        Routes.MORE      to nav.more,
    )

    val showBottomBar = currentDestination?.route != Routes.ONBOARDING
    val showTopBar = showBottomBar
    val isTopLevelTab = currentDestination?.route in bottomNavItems.map { it.route }

    val topBarTitle = routeTitle(currentDestination?.route)

    Scaffold(
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text(topBarTitle, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        if (!isTopLevelTab) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = S.common.back,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            Column {
                // Skip-to-content button (visually hidden, accessible via TalkBack/keyboard)
                TextButton(
                    onClick = { contentFocusRequester.requestFocus() },
                    modifier = Modifier
                        .height(1.dp)
                        .offset(y = (-1).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        a11y.skipToContent,
                        modifier = Modifier.semantics { },
                    )
                }
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
        }
    ) { innerPadding ->
        // Determine enter/exit transitions based on reduced motion preference
        val enterTransition = if (accessibilityPrefs.reducedMotion) EnterTransition.None
            else fadeIn(animationSpec = tween(250))
        val exitTransition = if (accessibilityPrefs.reducedMotion) ExitTransition.None
            else fadeOut(animationSpec = tween(250))

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .padding(innerPadding)
                .focusRequester(contentFocusRequester),
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }
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
            composable(Routes.METRIC_HISTORY) {
                MetricHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Resolves the localized title for the top app bar based on current route. */
@Composable
private fun routeTitle(route: String?): String = when (route) {
    Routes.DASHBOARD     -> S.nav.dashboard
    Routes.TOOLS         -> S.nav.tools
    Routes.MONITOR       -> S.nav.monitor
    Routes.LOGBOOK       -> S.nav.logbook
    Routes.MORE          -> S.nav.more
    Routes.TORCH         -> S.nav.torch
    Routes.CAMERA        -> S.nav.camera
    Routes.VIBRATION     -> S.nav.vibration
    Routes.MIC           -> S.nav.mic
    Routes.SENSORS       -> S.nav.sensors
    Routes.BATTERY       -> S.nav.battery
    Routes.RADIOS        -> S.nav.radios
    Routes.LOCKSCREEN    -> S.lock.title
    Routes.LINK          -> S.link.title
    Routes.FILE_META     -> S.nav.fileMeta
    Routes.SETTINGS      -> S.settings.title
    Routes.BUG           -> S.bug.title
    Routes.MANUAL        -> S.manual.title
    Routes.SEARCH        -> S.common.search
    Routes.METRIC_HISTORY -> S.nav.dashboard  // parameterized route; reuse Dashboard label
    else                  -> ""
}
