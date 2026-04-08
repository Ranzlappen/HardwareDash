package com.hardwaredash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hardwaredash.ui.navigation.NavGraph
import com.hardwaredash.ui.theme.HardwareDashTheme
import com.hardwaredash.ui.ticked.TickedReminderWorker
import com.hardwaredash.widget.WidgetUpdateWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic home screen widget updates
        WidgetUpdateWorker.schedule(this)

        // Ensure Ticked notification channel exists
        TickedReminderWorker.ensureChannel(this)

        setContent {
            HardwareDashTheme {
                NavGraph()
            }
        }
    }
}
