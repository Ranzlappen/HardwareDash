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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gadget.apps.AppRepository
import com.gadget.widget.folder.FolderWidgetController
import com.gadget.localization.LocalizationManager
import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.LaunchGateOutcome
import com.gadget.root.ui.FatalLaunchScreen
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.ThemePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject
import com.gadget.ui.navigation.NavGraph
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.logbook.LogbookReminderWorker
import com.gadget.widget.WidgetUpdateWorker

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

        // Initialize localization from persisted preference
        LocalizationManager.init(this)

        // Initialize accessibility preferences
        AccessibilityPreferencesManager.init(this)

        // Initialize theme preferences (color preset selection)
        ThemePreferencesManager.init(this)

        // Initialize OSMDroid map tile configuration (cache + user agent)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = File(cacheDir, "osmdroid")
            osmdroidBasePath = File(cacheDir, "osmdroid")
        }

        // Schedule periodic home screen widget updates
        WidgetUpdateWorker.schedule(this)

        // Force eager Hilt instantiation of the apps module so its
        // LauncherApps.Callback registers and an initial scan kicks off.
        appRepository.requestRefresh()

        // Ensure Logbook notification channel exists
        LogbookReminderWorker.ensureChannel(this)

        setContent {
            GadgetTheme {
                val outcome by produceState<LaunchGateOutcome?>(initialValue = null) {
                    value = launchGate.check()
                }
                when (val resolved = outcome) {
                    null -> LaunchSplash()
                    LaunchGateOutcome.Allowed -> NavGraph()
                    is LaunchGateOutcome.DeniedFatal -> FatalLaunchScreen(
                        reason = resolved.reason,
                        onExit = { finishAffinity() },
                        onOpenInstructions = { openCompanionInstructions() },
                    )
                }
            }
        }
    }

    private fun openCompanionInstructions() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(companionDetector.installInstructionsUrl)),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun LaunchSplash() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
