package com.hardwaredash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hardwaredash.localization.LocalizationManager
import org.osmdroid.config.Configuration
import java.io.File
import com.hardwaredash.ui.navigation.NavGraph
import com.hardwaredash.ui.theme.HardwareDashTheme
import com.hardwaredash.ui.logbook.LogbookReminderWorker
import com.hardwaredash.widget.WidgetUpdateWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize localization from persisted preference
        LocalizationManager.init(this)

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
            HardwareDashTheme {
                NavGraph()
            }
        }
    }
}
