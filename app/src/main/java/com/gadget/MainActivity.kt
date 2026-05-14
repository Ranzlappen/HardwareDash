package com.gadget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import com.gadget.apps.AppRepository
import com.gadget.localization.LocalizationManager
import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.LaunchGateOutcome
import com.gadget.root.ui.FatalLaunchScreen
import com.gadget.ui.logbook.LogbookReminderWorker
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.theme.ThemePreferencesManager
import com.gadget.widget.WidgetUpdateWorker
import com.gadget.widget.folder.FolderWidgetController
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.navigation.GadgetApp
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.navigation.navigateTopLevel
import dev.ranzlappen.gadget.core.navigation.placeholderScreen
import dev.ranzlappen.gadget.feature.dashboard.dashboardScreen
import dev.ranzlappen.gadget.feature.settings.settingsScreen
import dev.ranzlappen.gadget.feature.torch.torchScreen
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository

    // Eager-injected so its init { } collects Room flows and re-renders folder
    // widgets reactively for the lifetime of the process.
    @Inject lateinit var folderWidgetController: FolderWidgetController

    @Inject lateinit var launchGate: LaunchGate
    @Inject lateinit var companionDetector: CompanionModuleDetector

    // Phase 2 / Batch 1 — new typed user-preferences repository. Drives
    // the GadgetApp theme params reactively (dark mode, dynamic colour,
    // reduced-motion override, reduced-transparency override).
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bootstrapLegacyManagers()

        setContent {
            val outcome by produceState<LaunchGateOutcome?>(initialValue = null) {
                value = launchGate.check()
            }
            when (val resolved = outcome) {
                null -> GadgetTheme { LaunchSplash() }
                LaunchGateOutcome.Allowed -> {
                    val navController = rememberNavController()
                    val preferences by userPreferencesRepository.flow
                        .collectAsState(initial = UserPreferences())
                    val systemDark = isSystemInDarkTheme()
                    val useDarkTheme = when (preferences.darkThemeMode) {
                        DarkThemeMode.Light -> false
                        DarkThemeMode.Dark -> true
                        DarkThemeMode.FollowSystem -> systemDark
                    }
                    val reducedMotionOverride = when (preferences.reducedMotionOverride) {
                        TriStatePreference.On -> true
                        TriStatePreference.Off -> false
                        TriStatePreference.FollowSystem -> null
                    }
                    GadgetApp(
                        navController = navController,
                        useDarkTheme = useDarkTheme,
                        useDynamicColor = preferences.dynamicColor,
                        reducedMotionOverride = reducedMotionOverride,
                        reducedTransparency = preferences.reducedTransparency,
                    ) {
                        dashboardScreen(
                            onNavigate = { destination ->
                                // Top-level destinations (Settings, Sensors, …)
                                // route via the back-stack-trimming helper;
                                // sub-routes (Torch) use plain navigate.
                                if (destination in GadgetDestination.topLevel) {
                                    navController.navigateTopLevel(destination)
                                } else {
                                    navController.navigate(destination.route)
                                }
                            },
                        )
                        placeholderScreen(GadgetDestination.Sensors)
                        placeholderScreen(GadgetDestination.Actuators)
                        placeholderScreen(GadgetDestination.Automation)
                        settingsScreen()
                        torchScreen()
                    }
                }
                is LaunchGateOutcome.DeniedFatal -> GadgetTheme {
                    FatalLaunchScreen(
                        reason = resolved.reason,
                        onExit = { finishAffinity() },
                        onOpenInstructions = { openCompanionInstructions() },
                    )
                }
            }
        }
    }

    private fun bootstrapLegacyManagers() {
        LocalizationManager.init(this)
        AccessibilityPreferencesManager.init(this)
        ThemePreferencesManager.init(this)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = File(cacheDir, "osmdroid")
            osmdroidBasePath = File(cacheDir, "osmdroid")
        }

        WidgetUpdateWorker.schedule(this)
        appRepository.requestRefresh()
        LogbookReminderWorker.ensureChannel(this)
    }

    private fun openCompanionInstructions() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(companionDetector.installInstructionsUrl)),
            )
        }
    }
}

@Composable
private fun LaunchSplash() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
