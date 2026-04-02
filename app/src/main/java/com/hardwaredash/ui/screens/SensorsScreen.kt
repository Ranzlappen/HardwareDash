// CHANGE: Fix proximity/step counter display, add additional sensor types
// REASON: Proximity now shows NEAR/FAR, Step Counter shows session delta, added 9 more sensor types
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
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

private val SENSOR_SPECS = buildList {
    add(SensorSpec(Sensor.TYPE_ACCELEROMETER,               "Accelerometer",      "m/s²",  listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_GYROSCOPE,                   "Gyroscope",          "rad/s", listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_MAGNETIC_FIELD,              "Magnetometer",       "µT",    listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_ROTATION_VECTOR,             "Rotation Vector",    "",      listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_GRAVITY,                     "Gravity",            "m/s²",  listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_LINEAR_ACCELERATION,         "Linear Accel",       "m/s²",  listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_LIGHT,                       "Ambient Light",      "lux",   listOf("Lux")))
    add(SensorSpec(Sensor.TYPE_PROXIMITY,                   "Proximity",          "cm",    listOf("Dist")))
    add(SensorSpec(Sensor.TYPE_PRESSURE,                    "Barometer",          "hPa",   listOf("hPa")))
    add(SensorSpec(Sensor.TYPE_AMBIENT_TEMPERATURE,         "Temperature",        "°C",    listOf("Temp")))
    add(SensorSpec(Sensor.TYPE_RELATIVE_HUMIDITY,           "Humidity",           "%",     listOf("RH")))
    add(SensorSpec(Sensor.TYPE_STEP_COUNTER,                "Step Counter",       "steps", listOf("Steps")))
    // Additional sensors
    add(SensorSpec(Sensor.TYPE_GAME_ROTATION_VECTOR,        "Game Rotation",      "",      listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "Geo Rotation",       "",      listOf("X","Y","Z")))
    add(SensorSpec(Sensor.TYPE_STATIONARY_DETECT,           "Stationary Detect",  "",      listOf("State")))
    add(SensorSpec(Sensor.TYPE_MOTION_DETECT,               "Motion Detect",      "",      listOf("State")))
    add(SensorSpec(Sensor.TYPE_HEART_RATE,                  "Heart Rate",         "bpm",   listOf("BPM")))
    add(SensorSpec(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,  "Accel (Raw)",        "m/s²",  listOf("X","Y","Z","bX","bY","bZ")))
    add(SensorSpec(Sensor.TYPE_GYROSCOPE_UNCALIBRATED,      "Gyro (Raw)",         "rad/s", listOf("X","Y","Z","bX","bY","bZ")))
    add(SensorSpec(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, "Magneto (Raw)",      "µT",    listOf("X","Y","Z","bX","bY","bZ")))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add(SensorSpec(Sensor.TYPE_HINGE_ANGLE,             "Hinge Angle",        "°",     listOf("Angle")))
    }
}

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

    // Track initial step count for session delta
    var initialSteps by remember(spec.type) { mutableStateOf<Float?>(null) }

    // Get proximity max range for NEAR/FAR classification
    val proximityMax = remember(spec.type) {
        if (spec.type == Sensor.TYPE_PROXIMITY) {
            sm.getDefaultSensor(spec.type)?.maximumRange ?: 5f
        } else 0f
    }

    // Subscribe to sensor only when card is visible (always subscribed here)
    LaunchedEffect(spec.type) {
        sm.flowFor(spec.type).collect { v ->
            values  = v
            // track first axis for chart
            val chartVal = when (spec.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val raw = v.getOrElse(0) { 0f }
                    if (initialSteps == null) initialSteps = raw
                    raw - (initialSteps ?: raw)
                }
                else -> v.getOrElse(0) { 0f }
            }
            history = (history.drop(1) + chartVal)
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
                        when (spec.type) {
                            Sensor.TYPE_PROXIMITY -> {
                                val dist = values.getOrElse(0) { 0f }
                                val nearFar = if (dist < proximityMax) "NEAR" else "FAR"
                                "Dist: ${"%.1f".format(dist)} ${spec.unit}  ·  $nearFar"
                            }
                            Sensor.TYPE_STEP_COUNTER -> {
                                val raw = values.getOrElse(0) { 0f }
                                val session = raw - (initialSteps ?: raw)
                                "Session: ${session.toLong()} steps  ·  Total: ${raw.toLong()}"
                            }
                            Sensor.TYPE_STATIONARY_DETECT, Sensor.TYPE_MOTION_DETECT -> {
                                val v = values.getOrElse(0) { 0f }
                                if (v > 0.5f) "Detected" else "Not detected"
                            }
                            else -> {
                                spec.axisLabels.mapIndexed { i, lbl ->
                                    "$lbl: ${"%.3f".format(values.getOrElse(i) { 0f })} ${spec.unit}"
                                }.joinToString("  ·  ")
                            }
                        },
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
                        when (spec.type) {
                            Sensor.TYPE_STEP_COUNTER -> "Live chart — session steps"
                            Sensor.TYPE_PROXIMITY -> "Live chart — distance"
                            else -> "Live chart — ${spec.axisLabels.first()} axis"
                        },
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
