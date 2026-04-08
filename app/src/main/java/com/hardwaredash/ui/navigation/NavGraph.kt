package com.hardwaredash.ui.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.hardwaredash.ui.screens.*
import com.hardwaredash.ui.ticked.TickedScreen

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val TORCH        = "torch"
    const val CAMERA       = "camera"
    const val VIBRATION    = "vibration"
    const val MIC          = "mic"
    const val RADIOS       = "radios"
    const val SENSORS      = "sensors"
    const val LOCKSCREEN   = "lockscreen"
    const val BATTERY      = "battery"
    const val TICKED       = "ticked"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.TICKED,     "Ticked",    Icons.Filled.CheckCircle,    Icons.Outlined.CheckCircle),
    BottomNavItem(Routes.TORCH,      "Torch",     Icons.Filled.FlashlightOn,  Icons.Outlined.FlashlightOn),
    BottomNavItem(Routes.CAMERA,     "Camera",    Icons.Filled.CameraAlt,     Icons.Outlined.CameraAlt),
    BottomNavItem(Routes.VIBRATION,  "Vibration", Icons.Filled.Vibration,     Icons.Outlined.Vibration),
    BottomNavItem(Routes.MIC,        "Mic",       Icons.Filled.Mic,           Icons.Outlined.Mic),
    BottomNavItem(Routes.RADIOS,     "Radios",    Icons.Filled.Wifi,          Icons.Outlined.Wifi),
    BottomNavItem(Routes.SENSORS,    "Sensors",   Icons.Filled.Analytics,     Icons.Outlined.Analytics),
    BottomNavItem(Routes.BATTERY,    "Battery",   Icons.Filled.BatteryStd,    Icons.Outlined.BatteryStd),
    BottomNavItem(Routes.LOCKSCREEN, "Lock",      Icons.Filled.Lock,          Icons.Outlined.Lock),
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = NavigationBarDefaults.Elevation,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
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
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
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
            startDestination = Routes.TICKED,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TICKED)        { TickedScreen() }
            composable(Routes.TORCH)         { TorchScreen() }
            composable(Routes.CAMERA)        { CameraScreen() }
            composable(Routes.VIBRATION)     { VibrationScreen() }
            composable(Routes.MIC)           { MicScreen() }
            composable(Routes.RADIOS)        { RadiosScreen() }
            composable(Routes.SENSORS)       { SensorsScreen() }
            composable(Routes.LOCKSCREEN)    { LockScreenScreen() }
            composable(Routes.BATTERY)       { BatteryScreen() }
        }
    }
}
