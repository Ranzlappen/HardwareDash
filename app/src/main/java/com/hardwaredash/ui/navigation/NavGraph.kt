package com.hardwaredash.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon  = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
        }
    }
}
