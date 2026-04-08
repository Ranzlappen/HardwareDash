// CHANGE: Save/load vibration patterns, visual canvas pattern drawing
// REASON: Persist patterns via SharedPreferences, add finger-draw two-axis vibration interface
// DATE: 2026-04-06

package com.hardwaredash.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardwaredash.localization.S
import org.json.JSONArray
import org.json.JSONObject

// ─── Helper: get Vibrator from context regardless of API level ────────────────
private fun getVibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

// ─── Predefined patterns ──────────────────────────────────────────────────────
private data class VibPattern(val label: String, val effect: () -> VibrationEffect)

private val patterns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(
    VibPattern("Click")       { VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK) },
    VibPattern("Double Click") { VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK) },
    VibPattern("Heavy Click") { VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK) },
    VibPattern("Tick")        { VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK) },
) else emptyList()

// ─── Waveform step data ───────────────────────────────────────────────────────
private data class WaveformStepData(
    val amplitude: Float = 0.5f,
    val duration: Float = 150f,
)

// ─── SharedPreferences helpers for save/load ──────────────────────────────────
private const val PREFS_NAME = "vibration_patterns"
private const val PREFS_KEY = "saved_patterns"
private const val MAX_SAVED = 20

private fun savePatterns(context: Context, patterns: List<SavedPattern>) {
    val arr = JSONArray()
    patterns.take(MAX_SAVED).forEach { p ->
        val obj = JSONObject().apply {
            put("name", p.name)
            put("gapMs", p.gapMs.toDouble())
            put("loop", p.loop)
            val stepsArr = JSONArray()
            p.steps.forEach { s ->
                stepsArr.put(JSONObject().apply {
                    put("amp", s.amplitude.toDouble())
                    put("dur", s.duration.toDouble())
                })
            }
            put("steps", stepsArr)
        }
        arr.put(obj)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREFS_KEY, arr.toString()).apply()
}

private fun loadPatterns(context: Context): List<SavedPattern> {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val stepsArr = obj.getJSONArray("steps")
            SavedPattern(
                name = obj.getString("name"),
                gapMs = obj.getDouble("gapMs").toFloat(),
                loop = obj.getBoolean("loop"),
                steps = (0 until stepsArr.length()).map { j ->
                    val s = stepsArr.getJSONObject(j)
                    WaveformStepData(s.getDouble("amp").toFloat(), s.getDouble("dur").toFloat())
                },
            )
        }
    } catch (_: Exception) { emptyList() }
}

private data class SavedPattern(
    val name: String,
    val steps: List<WaveformStepData>,
    val gapMs: Float,
    val loop: Boolean,
)

// ─── Drawn point for canvas pattern ───────────────────────────────────────────
private data class DrawnPoint(val timeNorm: Float, val intensity: Float)

@Composable
fun VibrationScreen() {
    val context  = LocalContext.current
    val vibrator = remember { getVibrator(context) }

    // Dynamic waveform steps
    val steps = remember { mutableStateListOf(
        WaveformStepData(0.3f, 100f),
        WaveformStepData(0.8f, 200f),
        WaveformStepData(1.0f, 150f),
    ) }

    // Gap between steps (ms)
    var gapMs by remember { mutableFloatStateOf(50f) }

    // Loop toggle
    var loopEnabled by remember { mutableStateOf(false) }

    val hasAmplitude = vibrator.hasAmplitudeControl()

    // Save/Load state
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var savedPatterns by remember { mutableStateOf(loadPatterns(context)) }

    // Canvas drawing state
    val drawnPoints = remember { mutableStateListOf<DrawnPoint>() }
    var drawLoopEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(S.vibration.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (!hasAmplitude) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    S.vibration.noAmplitude,
                    modifier = Modifier.padding(12.dp),
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        // ── Predefined patterns ───────────────────────────────────────────────
        if (patterns.isNotEmpty()) {
            Text(S.vibration.predefinedEffects, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                patterns.forEach { p ->
                    ElevatedButton(onClick = {
                        vibrator.cancel()
                        vibrator.vibrate(p.effect())
                    }) { Text(p.label, maxLines = 1, softWrap = false) }
                }
            }
        }

        HorizontalDivider()

        // ── Custom waveform ───────────────────────────────────────────────────
        Text(S.vibration.customWaveform, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Amplitude: 0 = off, 1 = full\nDuration: ms  ·  Gap: pause between steps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // Speed presets
        Text(S.vibration.speedPresets, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Slow" to 200f, "Medium" to 80f, "Fast" to 30f, "Rapid" to 10f).forEach { (label, gap) ->
                FilterChip(
                    selected = gapMs == gap,
                    onClick  = { gapMs = gap },
                    label    = { Text(label) },
                )
            }
        }

        // Gap slider
        Text("Gap between steps: ${gapMs.toInt()} ms", style = MaterialTheme.typography.bodySmall)
        Slider(value = gapMs, onValueChange = { gapMs = it }, valueRange = 0f..500f)

        // Loop toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Loop waveform",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = loopEnabled, onCheckedChange = { loopEnabled = it })
        }

        // Dynamic steps
        steps.forEachIndexed { idx, step ->
            WaveformStep(
                label = "Step ${idx + 1}",
                amplitude = step.amplitude,
                duration  = step.duration,
                hasAmplitude = hasAmplitude,
                onAmpChange  = { steps[idx] = step.copy(amplitude = it) },
                onDurChange  = { steps[idx] = step.copy(duration = it) },
                onRemove     = if (steps.size > 1) ({ steps.removeAt(idx) }) else null,
            )
        }

        // Add/remove step buttons
        if (steps.size < 10) {
            OutlinedButton(
                onClick = { steps.add(WaveformStepData()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Step (${steps.size}/10)")
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick  = {
                vibrator.cancel()
                playWaveform(vibrator, steps.toList(), gapMs, loopEnabled, hasAmplitude)
            }
        ) { Text(if (loopEnabled) S.vibration.playLooping else S.vibration.playCustom, maxLines = 1, softWrap = false) }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onClick  = { vibrator.cancel() },
        ) { Text(S.vibration.stop) }

        // ── Save / Load ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.vibration.savePattern, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    savedPatterns = loadPatterns(context)
                    showLoadDialog = true
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.vibration.loadPattern, maxLines = 1, softWrap = false)
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // ── Visual Pattern Drawing (Canvas) ──────────────────────────────────
        // ══════════════════════════════════════════════════════════════════════
        Text(S.vibration.drawPattern, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            S.vibration.drawInstructions,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        val canvasBg = MaterialTheme.colorScheme.surfaceVariant
        val drawColor = MaterialTheme.colorScheme.primary
        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        val gridColor = Color.Gray.copy(alpha = 0.3f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .clip(MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            drawnPoints.clear()
                            val tNorm = (offset.x / size.width).coerceIn(0f, 1f)
                            val iNorm = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            drawnPoints.add(DrawnPoint(tNorm, iNorm))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val tNorm = (change.position.x / size.width).coerceIn(0f, 1f)
                            val iNorm = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            drawnPoints.add(DrawnPoint(tNorm, iNorm))
                        },
                    )
                }
        ) {
            drawRect(canvasBg, size = size)

            // Grid lines
            for (i in 1..3) {
                val y = size.height * i / 4f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
            }
            for (i in 1..3) {
                val x = size.width * i / 4f
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx())
            }

            if (drawnPoints.isNotEmpty()) {
                // Sort by time
                val sorted = drawnPoints.sortedBy { it.timeNorm }

                // Filled area
                val fillPath = Path().apply {
                    moveTo(sorted.first().timeNorm * size.width, size.height)
                    sorted.forEach { p ->
                        lineTo(p.timeNorm * size.width, size.height * (1f - p.intensity))
                    }
                    lineTo(sorted.last().timeNorm * size.width, size.height)
                    close()
                }
                drawPath(fillPath, fillColor)

                // Line
                val linePath = Path()
                sorted.forEachIndexed { i, p ->
                    val x = p.timeNorm * size.width
                    val y = size.height * (1f - p.intensity)
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, drawColor, style = Stroke(3.dp.toPx()))
            }
        }

        // Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0 ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text("2000 ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }

        // Draw loop toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Loop drawn pattern",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = drawLoopEnabled, onCheckedChange = { drawLoopEnabled = it })
        }

        // Draw action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    vibrator.cancel()
                    if (drawnPoints.isNotEmpty()) {
                        playDrawnPattern(vibrator, drawnPoints.toList(), drawLoopEnabled, hasAmplitude)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = drawnPoints.isNotEmpty(),
            ) { Text(S.vibration.playDrawn, maxLines = 1, softWrap = false) }

            OutlinedButton(
                onClick = { drawnPoints.clear() },
                modifier = Modifier.weight(1f),
            ) { Text(S.vibration.clearDrawing, maxLines = 1, softWrap = false) }
        }
    }

    // ── Save dialog ───────────────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(S.vibration.savePattern) },
            text = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text(S.vibration.patternName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveName.isNotBlank()) {
                            val pattern = SavedPattern(
                                name = saveName.trim(),
                                steps = steps.toList(),
                                gapMs = gapMs,
                                loop = loopEnabled,
                            )
                            savedPatterns = (listOf(pattern) + savedPatterns).take(MAX_SAVED)
                            savePatterns(context, savedPatterns)
                            saveName = ""
                            showSaveDialog = false
                        }
                    },
                    enabled = saveName.isNotBlank(),
                ) { Text(S.vibration.save) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(S.vibration.cancel) }
            },
        )
    }

    // ── Load dialog ───────────────────────────────────────────────────────────
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text(S.vibration.loadPattern) },
            text = {
                if (savedPatterns.isEmpty()) {
                    Text(S.vibration.noSavedPatterns, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        savedPatterns.forEachIndexed { idx, p ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${p.steps.size} steps · gap ${p.gapMs.toInt()}ms${if (p.loop) " · loop" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            // Load pattern
                                            steps.clear()
                                            steps.addAll(p.steps)
                                            gapMs = p.gapMs
                                            loopEnabled = p.loop
                                            showLoadDialog = false
                                        }) {
                                            Icon(Icons.Default.FileOpen, "Load")
                                        }
                                        IconButton(onClick = {
                                            savedPatterns = savedPatterns.toMutableList().also { it.removeAt(idx) }
                                            savePatterns(context, savedPatterns)
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) { Text(S.vibration.close) }
            },
        )
    }
}

// ─── Play waveform from steps ─────────────────────────────────────────────────
private fun playWaveform(
    vibrator: Vibrator,
    steps: List<WaveformStepData>,
    gapMs: Float,
    loop: Boolean,
    hasAmplitude: Boolean,
) {
    val timings    = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()
    steps.forEachIndexed { idx, step ->
        if (idx > 0) {
            timings.add(gapMs.toLong())
            amplitudes.add(0)
        }
        timings.add(step.duration.toLong())
        amplitudes.add(if (hasAmplitude) (step.amplitude * 255).toInt() else 255)
    }
    val repeatIdx = if (loop) 0 else -1
    vibrator.vibrate(
        VibrationEffect.createWaveform(
            timings.toLongArray(),
            amplitudes.toIntArray(),
            repeatIdx,
        )
    )
}

// ─── Play drawn canvas pattern ────────────────────────────────────────────────
private fun playDrawnPattern(
    vibrator: Vibrator,
    points: List<DrawnPoint>,
    loop: Boolean,
    hasAmplitude: Boolean,
) {
    if (points.isEmpty()) return
    val sorted = points.sortedBy { it.timeNorm }

    // Total duration is 2000ms. Sample at 50ms intervals.
    val totalMs = 2000L
    val sampleInterval = 50L
    val numSamples = (totalMs / sampleInterval).toInt()

    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()

    for (i in 0 until numSamples) {
        val tNorm = i.toFloat() / numSamples
        // Find surrounding points and interpolate intensity
        val intensity = interpolateIntensity(sorted, tNorm)
        timings.add(sampleInterval)
        amplitudes.add(if (hasAmplitude) (intensity * 255).toInt().coerceIn(0, 255) else if (intensity > 0.1f) 255 else 0)
    }

    val repeatIdx = if (loop) 0 else -1
    vibrator.vibrate(
        VibrationEffect.createWaveform(
            timings.toLongArray(),
            amplitudes.toIntArray(),
            repeatIdx,
        )
    )
}

private fun interpolateIntensity(points: List<DrawnPoint>, tNorm: Float): Float {
    if (points.isEmpty()) return 0f
    if (tNorm <= points.first().timeNorm) return points.first().intensity
    if (tNorm >= points.last().timeNorm) return points.last().intensity

    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        if (tNorm in p1.timeNorm..p2.timeNorm) {
            val frac = if (p2.timeNorm > p1.timeNorm) (tNorm - p1.timeNorm) / (p2.timeNorm - p1.timeNorm) else 0f
            return p1.intensity + frac * (p2.intensity - p1.intensity)
        }
    }
    return points.last().intensity
}

@Composable
private fun WaveformStep(
    label: String,
    amplitude: Float, duration: Float,
    hasAmplitude: Boolean,
    onAmpChange: (Float) -> Unit, onDurChange: (Float) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (onRemove != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove step", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (hasAmplitude) {
                Text("Amplitude: ${"%.0f".format(amplitude * 100)}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = amplitude, onValueChange = onAmpChange, valueRange = 0f..1f)
            }

            Text("Duration: ${duration.toInt()} ms", style = MaterialTheme.typography.bodySmall)
            Slider(value = duration, onValueChange = onDurChange, valueRange = 10f..2000f)
        }
    }
}
