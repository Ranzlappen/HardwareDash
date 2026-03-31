package com.hardwaredash.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

// ─── Sensor display descriptor ────────────────────────────────────────────────
private data class SensorSpec(
    val type: Int,
    val name: String,
    val unit: String,
    val axisLabels: List<String>,
)

private val SENSOR_SPECS = listOf(
    SensorSpec(Sensor.TYPE_ACCELEROMETER,         "Accelerometer",      "m/s²",  listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_GYROSCOPE,             "Gyroscope",          "rad/s", listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_MAGNETIC_FIELD,        "Magnetometer",       "µT",    listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_ROTATION_VECTOR,       "Rotation Vector",    "",      listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_GRAVITY,               "Gravity",            "m/s²",  listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_LINEAR_ACCELERATION,   "Linear Accel",       "m/s²",  listOf("X","Y","Z")),
    SensorSpec(Sensor.TYPE_LIGHT,                 "Ambient Light",      "lux",   listOf("Lux")),
    SensorSpec(Sensor.TYPE_PROXIMITY,             "Proximity",          "cm",    listOf("Dist")),
    SensorSpec(Sensor.TYPE_PRESSURE,              "Barometer",          "hPa",   listOf("hPa")),
    SensorSpec(Sensor.TYPE_AMBIENT_TEMPERATURE,   "Temperature",        "°C",    listOf("Temp")),
    SensorSpec(Sensor.TYPE_RELATIVE_HUMIDITY,     "Humidity",           "%",     listOf("RH")),
    SensorSpec(Sensor.TYPE_STEP_COUNTER,          "Step Counter",       "steps", listOf("Steps")),
)

// ─── Flow wrapper for SensorManager ──────────────────────────────────────────
private fun SensorManager.flowFor(type: Int): Flow<FloatArray> = callbackFlow {
    val sensor = getDefaultSensor(type) ?: run { close(); return@callbackFlow }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) { trySend(e.values.clone()) }
        override fun onAccuracyChanged(s: Sensor, a: Int) {}
    }
    registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    awaitClose { unregisterListener(listener) }
}

@Composable
fun SensorsScreen() {
    val context = LocalContext.current
    val sm      = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Which sensor is expanded to show chart
    var expandedType by remember { mutableIntStateOf(-1) }

    // Available sensors on this device
    val availableTypes = remember {
        SENSOR_SPECS.filter { sm.getDefaultSensor(it.type) != null }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Sensors  (${availableTypes.size} found)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(availableTypes) { spec ->
                SensorCard(
                    sm       = sm,
                    spec     = spec,
                    expanded = expandedType == spec.type,
                    onExpand = { expandedType = if (expandedType == spec.type) -1 else spec.type },
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    sm: SensorManager, spec: SensorSpec,
    expanded: Boolean, onExpand: () -> Unit,
) {
    var values  by remember(spec.type) { mutableStateOf(FloatArray(spec.axisLabels.size)) }
    var history by remember(spec.type) { mutableStateOf(List(80) { 0f }) }

    // Subscribe to sensor only when card is visible (always subscribed here)
    LaunchedEffect(spec.type) {
        sm.flowFor(spec.type).collect { v ->
            values  = v
            // track first axis for chart
            history = (history.drop(1) + (v.getOrElse(0) { 0f }))
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val chartBg   = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onExpand)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ── Header row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(spec.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        spec.axisLabels.mapIndexed { i, lbl ->
                            "$lbl: ${"%.3f".format(values.getOrElse(i) { 0f })} ${spec.unit}"
                        }.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            }

            // ── Chart (visible when expanded) ─────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Live chart — ${spec.axisLabels.first()} axis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val max   = history.maxOrNull()?.let { maxOf(it, 1f) } ?: 1f
                        val min   = history.minOrNull() ?: -1f
                        val range = (max - min).takeIf { it > 0f } ?: 1f
                        val pts   = history.size
                        val stepX = size.width / (pts - 1)

                        // Background
                        drawRect(chartBg, size = size)

                        // Zero line
                        val zeroY = size.height * (1 - (-min / range)).coerceIn(0f, 1f)
                        drawLine(
                            Color.Gray.copy(alpha = 0.4f),
                            Offset(0f, zeroY), Offset(size.width, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Line chart
                        val path = Path()
                        history.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height * (1 - ((v - min) / range)).coerceIn(0f, 1f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
                    }
                }
            }
        }
    }
}
