package com.hardwaredash.ui.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
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
import com.hardwaredash.ui.logbook.LogbookScreen

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
    const val LOGBOOK      = "logbook"
    const val SETTINGS     = "settings"
    const val FILE_META    = "file_meta"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.LOGBOOK,    "Logbook",   Icons.Filled.CheckCircle,    Icons.Outlined.CheckCircle),
    BottomNavItem(Routes.TORCH,      "Torch",     Icons.Filled.FlashlightOn,  Icons.Outlined.FlashlightOn),
    BottomNavItem(Routes.CAMERA,     "Camera",    Icons.Filled.CameraAlt,     Icons.Outlined.CameraAlt),
    BottomNavItem(Routes.VIBRATION,  "Vibration", Icons.Filled.Vibration,     Icons.Outlined.Vibration),
    BottomNavItem(Routes.MIC,        "Mic",       Icons.Filled.Mic,           Icons.Outlined.Mic),
    BottomNavItem(Routes.RADIOS,     "Radios",    Icons.Filled.Wifi,          Icons.Outlined.Wifi),
    BottomNavItem(Routes.SENSORS,    "Sensors",   Icons.Filled.Analytics,     Icons.Outlined.Analytics),
    BottomNavItem(Routes.BATTERY,    "Battery",   Icons.Filled.BatteryStd,    Icons.Outlined.BatteryStd),
    BottomNavItem(Routes.LOCKSCREEN, "Lock",      Icons.Filled.Lock,          Icons.Outlined.Lock),
    BottomNavItem(Routes.FILE_META,  "Files",     Icons.Filled.InsertDriveFile, Icons.Outlined.InsertDriveFile),
    BottomNavItem(Routes.SETTINGS,   "Settings",  Icons.Filled.Settings,      Icons.Outlined.Settings),
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = NavigationBarDefaults.Elevation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                        .horizontalScroll(rememberScrollState())
                        .defaultMinSize(minHeight = 80.dp)
                        .selectableGroup(),
                    verticalAlignment = Alignment.Top,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        Box(modifier = Modifier.width(82.dp)) {
                            this@Row.NavigationBarItem(
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGBOOK,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGBOOK)       { LogbookScreen() }
            composable(Routes.TORCH)         { TorchScreen() }
            composable(Routes.CAMERA)        { CameraScreen() }
            composable(Routes.VIBRATION)     { VibrationScreen() }
            composable(Routes.MIC)           { MicScreen() }
            composable(Routes.RADIOS)        { RadiosScreen() }
            composable(Routes.SENSORS)       { SensorsScreen() }
            composable(Routes.LOCKSCREEN)    { LockScreenScreen() }
            composable(Routes.BATTERY)       { BatteryScreen() }
            composable(Routes.FILE_META)     { FileMetadataScreen() }
            composable(Routes.SETTINGS)      { SettingsScreen() }
        }
    }
}
