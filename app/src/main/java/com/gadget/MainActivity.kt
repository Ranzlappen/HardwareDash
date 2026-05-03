package com.gadget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gadget.localization.LocalizationManager
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.ThemePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import java.io.File
import com.gadget.ui.navigation.NavGraph
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.logbook.LogbookReminderWorker
import com.gadget.widget.WidgetUpdateWorker

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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

        // Ensure Logbook notification channel exists
        LogbookReminderWorker.ensureChannel(this)

        setContent {
            GadgetTheme {
                NavGraph()
            }
        }
    }
}
