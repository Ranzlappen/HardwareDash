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
import com.gadget.apps.AppRepository
import com.gadget.localization.LocalizationManager
import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.LaunchGateOutcome
import com.gadget.root.ui.FatalLaunchScreen
import androidx.navigation.compose.rememberNavController
import com.gadget.ui.logbook.LogbookReminderWorker
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.theme.ThemePreferencesManager
import com.gadget.widget.WidgetUpdateWorker
import com.gadget.widget.folder.FolderWidgetController
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.navigation.GadgetAppShell
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.navigation.navigateTopLevel
import dev.ranzlappen.gadget.core.navigation.placeholderScreen
import dev.ranzlappen.gadget.feature.dashboard.dashboardScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bootstrapLegacyManagers()

        setContent {
            GadgetTheme {
                val outcome by produceState<LaunchGateOutcome?>(initialValue = null) {
                    value = launchGate.check()
                }
                when (val resolved = outcome) {
                    null -> LaunchSplash()
                    LaunchGateOutcome.Allowed -> {
                        val navController = rememberNavController()
                        GadgetAppShell(navController = navController) {
                            dashboardScreen(
                                onNavigate = { destination ->
                                    navController.navigateTopLevel(destination)
                                },
                            )
                            placeholderScreen(GadgetDestination.Sensors)
                            placeholderScreen(GadgetDestination.Actuators)
                            placeholderScreen(GadgetDestination.Automation)
                            placeholderScreen(GadgetDestination.Settings)
                        }
                    }
                    is LaunchGateOutcome.DeniedFatal -> FatalLaunchScreen(
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
