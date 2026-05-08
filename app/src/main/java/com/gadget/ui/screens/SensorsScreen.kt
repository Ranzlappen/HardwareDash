// CHANGE: Multi-axis charts for all sensor values, clipboard copy button
// REASON: Show all available metrics per sensor, allow copying readings to clipboard
// DATE: 2026-04-06

package com.gadget.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import com.gadget.root.ui.SensorsRootExtrasSection
import com.gadget.ui.components.AccessibleCanvas
import com.gadget.ui.components.DashCard
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import com.gadget.ui.theme.LocalAccessibilityPreferences
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

// ─── Axis colors for multi-line charts ────────────────────────────────────────
private val axisColors = listOf(
    Color(0xFF00BCD4), // Cyan  (axis 0 / X)
    Color(0xFF4CAF50), // Green (axis 1 / Y)
    Color(0xFFFFC107), // Amber (axis 2 / Z)
    Color(0xFF9C27B0), // Purple (bias X)
    Color(0xFFFF5722), // Deep Orange (bias Y)
    Color(0xFF2196F3), // Blue (bias Z)
)

@Composable
fun SensorsScreen() {
    ScreenAnnouncement(S.accessibility.sensorsScreen)
    val context = LocalContext.current
    val sm      = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val strSensorReadingsCopied = S.sensors.sensorReadingsCopied

    // Which sensor is expanded to show chart
    var expandedType by remember { mutableIntStateOf(-1) }

    // Available sensors on this device
    val availableTypes = remember {
        SENSOR_SPECS.filter { sm.getDefaultSensor(it.type) != null }
    }

    // Shared map of current sensor values for clipboard copy
    val sensorValues = remember { mutableStateMapOf<Int, FloatArray>() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) { },
            ) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "${S.sensors.title}  (${availableTypes.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.sectionHeading(),
                )
            }
            IconButton(onClick = {
                val text = buildString {
                    appendLine("Gadget — Sensor Readings")
                    appendLine("─".repeat(40))
                    availableTypes.forEach { spec ->
                        val vals = sensorValues[spec.type]
                        if (vals != null) {
                            val formatted = when (spec.type) {
                                Sensor.TYPE_PROXIMITY -> {
                                    val dist = vals.getOrElse(0) { 0f }
                                    val maxRange = sm.getDefaultSensor(spec.type)?.maximumRange ?: 5f
                                    val nearFar = if (dist < maxRange) "NEAR" else "FAR"
                                    "Dist: ${"%.1f".format(dist)} ${spec.unit} ($nearFar)"
                                }
                                Sensor.TYPE_STATIONARY_DETECT, Sensor.TYPE_MOTION_DETECT -> {
                                    if (vals.getOrElse(0) { 0f } > 0.5f) "Detected" else "Not detected"
                                }
                                else -> {
                                    spec.axisLabels.mapIndexed { i, lbl ->
                                        "$lbl=${"%.3f".format(vals.getOrElse(i) { 0f })}"
                                    }.joinToString("  ") + if (spec.unit.isNotEmpty()) " ${spec.unit}" else ""
                                }
                            }
                            appendLine("${spec.name}: $formatted")
                        }
                    }
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Sensor Readings", text))
                Toast.makeText(context, strSensorReadingsCopied, Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, S.accessibility.copyReadings)
            }
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
                    onValuesUpdate = { sensorValues[spec.type] = it },
                )
            }
            item { SensorsRootExtrasSection() }
        }
    }
}

@Composable
private fun SensorCard(
    sm: SensorManager, spec: SensorSpec,
    expanded: Boolean, onExpand: () -> Unit,
    onValuesUpdate: (FloatArray) -> Unit,
) {
    val axisCount = spec.axisLabels.size
    var values  by remember(spec.type) { mutableStateOf(FloatArray(axisCount)) }
    // History stores FloatArray per sample for all axes
    var history by remember(spec.type) { mutableStateOf(List(80) { FloatArray(axisCount) }) }

    // Track initial step count for session delta
    var initialSteps by remember(spec.type) { mutableStateOf<Float?>(null) }

    // Get proximity max range for NEAR/FAR classification
    val proximityMax = remember(spec.type) {
        if (spec.type == Sensor.TYPE_PROXIMITY) {
            sm.getDefaultSensor(spec.type)?.maximumRange ?: 5f
        } else 0f
    }

    // Subscribe to sensor
    LaunchedEffect(spec.type) {
        sm.flowFor(spec.type).collect { v ->
            values = v
            onValuesUpdate(v)
            // Build chart sample
            val sample = when (spec.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val raw = v.getOrElse(0) { 0f }
                    if (initialSteps == null) initialSteps = raw
                    floatArrayOf(raw - (initialSteps ?: raw))
                }
                else -> FloatArray(axisCount) { i -> v.getOrElse(i) { 0f } }
            }
            history = (history.drop(1) + listOf(sample))
        }
    }

    val chartBg = MaterialTheme.colorScheme.surfaceVariant

    DashCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onExpand,
        contentPadding = 12.dp,
        verticalArrangement = Arrangement.Top,
    ) {
            // ── Header row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    contentDescription = if (expanded) S.accessibility.collapseSection else S.accessibility.expandSection
                )
            }

            // ── Chart (visible when expanded) ─────────────────────────────
            val reducedMotion = LocalAccessibilityPreferences.current.reducedMotion
            AnimatedVisibility(
                visible = expanded,
                enter = if (reducedMotion) EnterTransition.None else fadeIn() + expandVertically(),
                exit = if (reducedMotion) ExitTransition.None else fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (spec.type) {
                            Sensor.TYPE_STEP_COUNTER -> "Live chart — session steps"
                            Sensor.TYPE_PROXIMITY -> "Live chart — distance"
                            else -> {
                                val axesLabel = spec.axisLabels.joinToString(", ")
                                "Live chart — $axesLabel"
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))

                    // Determine how many axes to chart
                    val chartAxes = if (spec.type == Sensor.TYPE_STEP_COUNTER) 1 else axisCount

                    val chartDescription = S.accessibility.sensorChartDesc(
                        spec.name,
                        spec.axisLabels.mapIndexed { i, lbl ->
                            "$lbl=${"%.3f".format(values.getOrElse(i) { 0f })}"
                        }.joinToString(", ")
                    )

                    AccessibleCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.5f)
                            .clip(MaterialTheme.shapes.medium),
                        contentDescription = chartDescription,
                    ) {
                        // Find global min/max across all axes
                        var globalMin = Float.MAX_VALUE
                        var globalMax = Float.MIN_VALUE
                        for (sample in history) {
                            for (a in 0 until chartAxes.coerceAtMost(sample.size)) {
                                val v = sample.getOrElse(a) { 0f }
                                if (v < globalMin) globalMin = v
                                if (v > globalMax) globalMax = v
                            }
                        }
                        if (globalMin == Float.MAX_VALUE) globalMin = -1f
                        if (globalMax == Float.MIN_VALUE) globalMax = 1f
                        val range = (globalMax - globalMin).takeIf { it > 0f } ?: 1f
                        val pts = history.size
                        val stepX = size.width / (pts - 1)

                        // Background
                        drawRect(chartBg, size = size)

                        // Zero line
                        val zeroY = size.height * (1 - (-globalMin / range)).coerceIn(0f, 1f)
                        drawLine(
                            Color.Gray.copy(alpha = 0.4f),
                            Offset(0f, zeroY), Offset(size.width, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw one line per axis
                        for (a in 0 until chartAxes) {
                            val lineColor = axisColors.getOrElse(a) { Color.White }
                            val path = Path()
                            history.forEachIndexed { i, sample ->
                                val v = sample.getOrElse(a) { 0f }
                                val x = i * stepX
                                val y = size.height * (1 - ((v - globalMin) / range)).coerceIn(0f, 1f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
                        }
                    }

                    // Axis color legend
                    if (chartAxes > 1) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (a in 0 until chartAxes) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(Modifier.size(8.dp).semantics { this.contentDescription = "" }) {
                                        drawCircle(axisColors.getOrElse(a) { Color.White })
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        spec.axisLabels.getOrElse(a) { "?" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}
