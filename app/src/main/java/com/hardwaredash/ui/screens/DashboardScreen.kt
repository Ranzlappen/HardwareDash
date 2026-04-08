// CHANGE: Live real-time metric cards on dashboard
// REASON: Show live sensor/battery/wifi data on each feature tile
// DATE: 2026-04-06

package com.hardwaredash.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hardwaredash.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlin.math.sqrt

// ─── Data model for each feature tile ────────────────────────────────────────
private data class FeatureCard(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val available: Boolean = true,
)

private val features = listOf(
    FeatureCard(Routes.TICKED,       "Ticked",        "Log & process tracker",       Icons.Default.CheckCircle),
    FeatureCard(Routes.TORCH,        "Torch",         "Toggle flash LED",            Icons.Default.FlashlightOn),
    FeatureCard(Routes.CAMERA,       "Camera",        "Preview & capture",           Icons.Default.CameraAlt),
    FeatureCard(Routes.VIBRATION,    "Vibration",     "Patterns & waveforms",        Icons.Default.Vibration),
    FeatureCard(Routes.MIC,          "Microphone",    "Live amplitude meter",        Icons.Default.Mic),
    FeatureCard(Routes.RADIOS,       "Radios",        "WiFi · BT · NFC status",      Icons.Default.Wifi),
    FeatureCard(Routes.SENSORS,      "Sensors",       "Gyro · Accel · Light…",       Icons.Default.Analytics),
    FeatureCard(Routes.NOTIFICATIONS,"Notifications", "Rich + heads-up demos",       Icons.Default.Notifications),
    FeatureCard(Routes.LOCKSCREEN,   "Lock Screen",   "Admin lock + overlay",        Icons.Default.Lock),
    FeatureCard(Routes.BATTERY,     "Battery",       "Health · Temp · Charging",    Icons.Default.BatteryStd),
)

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current

    // Live metrics
    val batteryMetric = rememberBatteryMetric(context)
    val accelMetric = rememberAccelMetric(context)
    val wifiMetric = rememberWifiMetric(context)
    val lightMetric = rememberLightMetric(context)

    val liveMetrics = mapOf(
        Routes.BATTERY to batteryMetric,
        Routes.SENSORS to accelMetric,
        Routes.RADIOS to wifiMetric,
        Routes.MIC to lightMetric,
        Routes.TICKED to "Offline tracker",
        Routes.TORCH to "Tap to toggle",
        Routes.CAMERA to "Ready",
        Routes.VIBRATION to "Motor ready",
        Routes.NOTIFICATIONS to "Ready",
        Routes.LOCKSCREEN to "Secure",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Text(
            text       = "HardwareDash",
            style      = MaterialTheme.typography.headlineLarge,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text  = "Tap a module to interact with your device hardware.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(20.dp))

        // ── Feature grid ─────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns         = GridCells.Fixed(2),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding  = PaddingValues(bottom = 16.dp),
        ) {
            items(features) { card ->
                FeatureTile(
                    card = card,
                    liveMetric = liveMetrics[card.route],
                    onClick = { navController.navigate(card.route) },
                )
            }
        }
    }
}

@Composable
private fun FeatureTile(card: FeatureCard, liveMetric: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.Start,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = card.icon,
                        contentDescription = card.title,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }
            Column {
                Text(
                    text       = card.title,
                    style      = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text  = card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!liveMetric.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = liveMetric,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Live metric composables ──────────────────────────────────────────────────

@Composable
private fun rememberBatteryMetric(context: Context): String {
    var metric by remember { mutableStateOf("--") }
    LaunchedEffect(Unit) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val pct = if (scale > 0) (level * 100) / scale else level
                val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    else -> "Discharging"
                }
                metric = "$pct% · $status"
            }
            delay(3000L)
        }
    }
    return metric
}

@Composable
private fun rememberAccelMetric(context: Context): String {
    var metric by remember { mutableStateOf("--") }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            metric = "N/A"
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                metric = "${"%.1f".format(mag)} m/s²"
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    return metric
}

@Composable
private fun rememberWifiMetric(context: Context): String {
    var metric by remember { mutableStateOf("--") }
    LaunchedEffect(Unit) {
        while (true) {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wm.isWifiEnabled) {
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                val ssid = info?.ssid?.removeSurrounding("\"") ?: ""
                metric = if (ssid.isNotBlank() && ssid != "<unknown ssid>") ssid else "No network"
            } else {
                metric = "WiFi off"
            }
            delay(3000L)
        }
    }
    return metric
}

@Composable
private fun rememberLightMetric(context: Context): String {
    var metric by remember { mutableStateOf("Tap to monitor") }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                metric = "${"%.0f".format(e.values[0])} lux"
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    return metric
}
