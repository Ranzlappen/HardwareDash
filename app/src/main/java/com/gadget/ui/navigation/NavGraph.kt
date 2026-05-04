package com.gadget.ui.navigation

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gadget.ui.apps.FolderEditorScreen
import com.gadget.ui.charts.MetricHistoryScreen
import com.gadget.ui.link.LinkScreen
import com.gadget.ui.logbook.LogbookScreen
import com.gadget.ui.onboarding.OnboardingScreen
import com.gadget.ui.screens.BatteryScreen
import com.gadget.ui.screens.BugReportScreen
import com.gadget.ui.screens.CameraScreen
import com.gadget.ui.screens.FileMetadataScreen
import com.gadget.ui.screens.LockScreenScreen
import com.gadget.ui.screens.ManualScreen
import com.gadget.ui.screens.MicScreen
import com.gadget.ui.screens.AppsScreen
import com.gadget.ui.screens.RadiosScreen
import com.gadget.ui.screens.SensorsScreen
import com.gadget.ui.screens.SettingsScreen
import com.gadget.ui.screens.TorchScreen
import com.gadget.ui.screens.VibrationScreen
import com.gadget.ui.search.SearchScreen
import com.gadget.ui.theme.LocalAccessibilityPreferences

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    // ── Tools ──
    const val TORCH        = "torch"
    const val CAMERA       = "camera"
    const val VIBRATION    = "vibration"
    const val MIC          = "mic"

    // ── Monitor ──
    const val SENSORS      = "sensors"
    const val BATTERY      = "battery"
    const val RADIOS       = "radios"

    // ── Logs ──
    const val LOGBOOK      = "logbook"

    // ── Other features ──
    const val LOCKSCREEN   = "lockscreen"
    const val LINK         = "link"
    const val FILE_META    = "file_meta"
    const val SETTINGS     = "settings"
    const val BUG          = "bug"
    const val MANUAL       = "manual"
    const val APPS         = "apps"
    const val APPS_FOLDER_EDIT = "apps/folder/{folderId}"
    fun appsFolderEdit(id: Long) = "apps/folder/$id"

    // ── Onboarding ──
    const val ONBOARDING   = "onboarding"

    // ── Charts ──
    const val METRIC_HISTORY = "metric_history/{metricKey}"
    fun metricHistory(metricKey: String) = "metric_history/$metricKey"

    // ── Global Search ──
    const val SEARCH       = "search"
}

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val hasSeenOnboarding = remember {
        context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
            .getBoolean("has_seen_onboarding", false)
    }
    val startDest = if (hasSeenOnboarding) Routes.TORCH else Routes.ONBOARDING

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val accessibilityPrefs = LocalAccessibilityPreferences.current

    // Hide the rail during onboarding so the welcome flow can take the
    // full width without competing affordances.
    val showRail = currentRoute != null && currentRoute != Routes.ONBOARDING

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                SideRail(
                    currentRoute = currentRoute,
                    onItemClick = navigateTo,
                )
            }

            // Slide+fade transitions feel responsive without being distracting.
            // Reduced motion turns them off entirely.
            val enter = if (accessibilityPrefs.reducedMotion) EnterTransition.None
                else slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { it / 8 },
                ) + fadeIn(animationSpec = tween(220))
            val exit = if (accessibilityPrefs.reducedMotion) ExitTransition.None
                else slideOutHorizontally(
                    animationSpec = tween(180),
                    targetOffsetX = { -it / 12 },
                ) + fadeOut(animationSpec = tween(180))

            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                enterTransition = { enter },
                exitTransition = { exit },
                popEnterTransition = { enter },
                popExitTransition = { exit },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onComplete = {
                        navController.navigate(Routes.MANUAL) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    })
                }
                composable(Routes.TORCH)      { TorchScreen() }
                composable(Routes.CAMERA)     { CameraScreen() }
                composable(Routes.VIBRATION)  { VibrationScreen() }
                composable(Routes.MIC)        { MicScreen() }
                composable(Routes.SENSORS)    { SensorsScreen() }
                composable(Routes.BATTERY)    { BatteryScreen() }
                composable(Routes.RADIOS)     { RadiosScreen() }
                composable(Routes.LOGBOOK)    { LogbookScreen() }
                composable(Routes.LOCKSCREEN) { LockScreenScreen() }
                composable(Routes.LINK)       { LinkScreen() }
                composable(Routes.FILE_META)  { FileMetadataScreen() }
                composable(Routes.SETTINGS)   { SettingsScreen() }
                composable(Routes.BUG)        { BugReportScreen() }
                composable(Routes.MANUAL)     { ManualScreen() }
                composable(Routes.APPS) {
                    AppsScreen(
                        onFolderClick = { id ->
                            navController.navigate(Routes.appsFolderEdit(id))
                        },
                    )
                }
                composable(
                    route = Routes.APPS_FOLDER_EDIT,
                    arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
                ) {
                    FolderEditorScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.METRIC_HISTORY) {
                    MetricHistoryScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onNavigate = navigateTo,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
