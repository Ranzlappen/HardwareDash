package com.hardwaredash.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.hardwaredash.ui.screens.*

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val DASHBOARD    = "dashboard"
    const val TORCH        = "torch"
    const val CAMERA       = "camera"
    const val VIBRATION    = "vibration"
    const val MIC          = "mic"
    const val RADIOS       = "radios"
    const val SENSORS      = "sensors"
    const val NOTIFICATIONS = "notifications"
    const val LOCKSCREEN   = "lockscreen"
    const val BATTERY      = "battery"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD,    "Home",    Icons.Default.Dashboard),
    BottomNavItem(Routes.TORCH,        "Torch",   Icons.Default.FlashlightOn),
    BottomNavItem(Routes.CAMERA,       "Camera",  Icons.Default.CameraAlt),
    BottomNavItem(Routes.VIBRATION,    "Vibrate", Icons.Default.Vibration),
    BottomNavItem(Routes.MIC,          "Mic",     Icons.Default.Mic),
    BottomNavItem(Routes.RADIOS,       "Radios",  Icons.Default.Wifi),
    BottomNavItem(Routes.SENSORS,      "Sensors", Icons.Default.Analytics),
    BottomNavItem(Routes.NOTIFICATIONS,"Notifs",  Icons.Default.Notifications),
    BottomNavItem(Routes.LOCKSCREEN,   "Lock",    Icons.Default.Lock),
    BottomNavItem(Routes.BATTERY,     "Battery", Icons.Default.BatteryStd),
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedIndex = bottomNavItems.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            Surface(
                color = NavigationBarDefaults.containerColor,
                tonalElevation = NavigationBarDefaults.Elevation,
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 8.dp,
                    containerColor = NavigationBarDefaults.containerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {},
                ) {
                    bottomNavItems.forEachIndexed { idx, item ->
                        Tab(
                            selected = idx == selectedIndex,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp),
                            ) {
                                Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp))
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD)     { DashboardScreen(navController) }
            composable(Routes.TORCH)         { TorchScreen() }
            composable(Routes.CAMERA)        { CameraScreen() }
            composable(Routes.VIBRATION)     { VibrationScreen() }
            composable(Routes.MIC)           { MicScreen() }
            composable(Routes.RADIOS)        { RadiosScreen() }
            composable(Routes.SENSORS)       { SensorsScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.LOCKSCREEN)    { LockScreenScreen() }
            composable(Routes.BATTERY)      { BatteryScreen() }
        }
    }
}
