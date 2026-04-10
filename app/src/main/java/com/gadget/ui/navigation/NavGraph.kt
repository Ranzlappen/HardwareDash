package com.gadget.ui.navigation

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
import com.gadget.localization.S
import com.gadget.ui.screens.*
import com.gadget.ui.logbook.LogbookScreen
import com.gadget.ui.link.LinkScreen

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
    const val LINK         = "link"
    const val FILE_META    = "file_meta"
    const val BUG          = "bug"
}

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.LOGBOOK,    Icons.Filled.CheckCircle,    Icons.Outlined.CheckCircle),
    BottomNavItem(Routes.TORCH,      Icons.Filled.FlashlightOn,  Icons.Outlined.FlashlightOn),
    BottomNavItem(Routes.CAMERA,     Icons.Filled.CameraAlt,     Icons.Outlined.CameraAlt),
    BottomNavItem(Routes.VIBRATION,  Icons.Filled.Vibration,     Icons.Outlined.Vibration),
    BottomNavItem(Routes.MIC,        Icons.Filled.Mic,           Icons.Outlined.Mic),
    BottomNavItem(Routes.RADIOS,     Icons.Filled.Wifi,          Icons.Outlined.Wifi),
    BottomNavItem(Routes.SENSORS,    Icons.Filled.Analytics,     Icons.Outlined.Analytics),
    BottomNavItem(Routes.BATTERY,    Icons.Filled.BatteryStd,    Icons.Outlined.BatteryStd),
    BottomNavItem(Routes.LOCKSCREEN, Icons.Filled.Lock,          Icons.Outlined.Lock),
    BottomNavItem(Routes.LINK,       Icons.Filled.Link,          Icons.Outlined.Link),
    BottomNavItem(Routes.FILE_META,  Icons.Filled.InsertDriveFile, Icons.Outlined.InsertDriveFile),
    BottomNavItem(Routes.SETTINGS,   Icons.Filled.Settings,      Icons.Outlined.Settings),
    BottomNavItem(Routes.BUG,        Icons.Filled.BugReport,     Icons.Outlined.BugReport),
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Resolve localized labels inside composable scope
    val nav = S.nav
    val navLabels = mapOf(
        Routes.LOGBOOK    to nav.logbook,
        Routes.TORCH      to nav.torch,
        Routes.CAMERA     to nav.camera,
        Routes.VIBRATION  to nav.vibration,
        Routes.MIC        to nav.mic,
        Routes.RADIOS     to nav.radios,
        Routes.SENSORS    to nav.sensors,
        Routes.BATTERY    to nav.battery,
        Routes.LOCKSCREEN to nav.lock,
        Routes.LINK       to nav.link,
        Routes.FILE_META  to nav.fileMeta,
        Routes.SETTINGS   to nav.settings,
        Routes.BUG        to nav.bug,
    )

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
                        val label = navLabels[item.route] ?: item.route
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
            composable(Routes.LINK)          { LinkScreen() }
            composable(Routes.BATTERY)       { BatteryScreen() }
            composable(Routes.FILE_META)     { FileMetadataScreen() }
            composable(Routes.SETTINGS)      { SettingsScreen() }
            composable(Routes.BUG)           { BugReportScreen() }
        }
    }
}
