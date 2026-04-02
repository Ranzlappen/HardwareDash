// CHANGE: New Battery monitoring screen with comprehensive metrics
// REASON: Add dedicated Battery section showing charging speed, health, temperature, etc.
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun BatteryScreen() {
    val context = LocalContext.current
    val bm = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    var level       by remember { mutableIntStateOf(0) }
    var status      by remember { mutableStateOf("Unknown") }
    var plugType    by remember { mutableStateOf("None") }
    var health      by remember { mutableStateOf("Unknown") }
    var temperature by remember { mutableFloatStateOf(0f) }
    var voltage     by remember { mutableFloatStateOf(0f) }
    var technology  by remember { mutableStateOf("—") }
    var currentNow  by remember { mutableIntStateOf(0) }
    var currentAvg  by remember { mutableIntStateOf(0) }
    var chargeCounter by remember { mutableIntStateOf(0) }
    var energyCounter by remember { mutableLongStateOf(0L) }
    var chargeTimeRemaining by remember { mutableLongStateOf(-1L) }
    var currentHistory by remember { mutableStateOf(List(80) { 0f }) }

    LaunchedEffect(Unit) {
        while (true) {
            // Sticky broadcast — no receiver registration needed
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val lvl  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                level = if (scale > 0) (lvl * 100) / scale else lvl

                status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING     -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING  -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL          -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING  -> "Not Charging"
                    else -> "Unknown"
                }

                plugType = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_AC       -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Unplugged"
                }

                health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                    BatteryManager.BATTERY_HEALTH_GOOD            -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT        -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD             -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE    -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_COLD             -> "Cold"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                    else -> "Unknown"
                }

                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                voltage     = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
                technology  = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"
            }

            // BatteryManager properties
            currentNow    = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            currentAvg    = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            energyCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            chargeTimeRemaining = bm.computeChargeTimeRemaining()

            // Track current draw for chart (convert µA to mA)
            val mA = currentNow / 1000f
            currentHistory = (currentHistory.drop(1) + mA)

            delay(2000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.BatteryStd, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("Battery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // ── Level + status hero card ──────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "$level%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        level > 50 -> MaterialTheme.colorScheme.primary
                        level > 20 -> Color(0xFFFFC107)
                        else       -> MaterialTheme.colorScheme.error
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$status  ·  $plugType",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                if (chargeTimeRemaining > 0) {
                    val mins = chargeTimeRemaining / 1000 / 60
                    val hrs  = mins / 60
                    val rem  = mins % 60
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Est. charge time: ${hrs}h ${rem}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        // ── Detail rows ───────────────────────────────────────────────────
        BatteryRow("Health", health)
        BatteryRow("Temperature", "${"%.1f".format(temperature)} °C")
        BatteryRow("Voltage", "${"%.3f".format(voltage)} V")
        BatteryRow("Technology", technology)
        BatteryRow("Current (now)", "${currentNow / 1000} mA")
        BatteryRow("Current (avg)", "${currentAvg / 1000} mA")

        if (chargeCounter > 0) {
            BatteryRow("Remaining capacity", "${"%.1f".format(chargeCounter / 1000f)} mAh")
        }
        if (energyCounter > 0) {
            BatteryRow("Energy counter", "${"%.2f".format(energyCounter / 1_000_000f)} mWh")
        }

        HorizontalDivider()

        // ── Current draw chart ────────────────────────────────────────────
        Text(
            "Current Draw (mA) — live",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        val lineColor = MaterialTheme.colorScheme.primary
        val chartBg   = MaterialTheme.colorScheme.surfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val max   = currentHistory.maxOrNull()?.let { maxOf(abs(it), 1f) } ?: 1f
            val min   = currentHistory.minOrNull() ?: -1f
            val range = (max - min).takeIf { it > 0f } ?: 1f
            val pts   = currentHistory.size
            val stepX = size.width / (pts - 1)

            drawRect(chartBg, size = size)

            // Zero line
            val zeroY = size.height * (1 - (-min / range)).coerceIn(0f, 1f)
            drawLine(
                Color.Gray.copy(alpha = 0.4f),
                Offset(0f, zeroY), Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            val path = Path()
            currentHistory.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height * (1 - ((v - min) / range)).coerceIn(0f, 1f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
        }

        Text(
            "Updates every 2 seconds. Negative current = discharging.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun BatteryRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
