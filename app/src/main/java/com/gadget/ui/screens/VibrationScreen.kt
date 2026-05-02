// CHANGE: Save/load vibration patterns, visual canvas pattern drawing
// REASON: Persist patterns via SharedPreferences, add finger-draw two-axis vibration interface
// DATE: 2026-04-06

package com.gadget.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import com.gadget.ui.components.AccessibleCanvas
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.SliderWithInput
import com.gadget.ui.components.minimumTouchTarget
import com.gadget.ui.components.sectionHeading
import com.gadget.widget.DrawnPatternUtils
import com.gadget.widget.DrawnPoint
import com.gadget.widget.SavedDrawnPattern
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

// DrawnPoint is imported from com.gadget.widget.DrawnPatternUtils

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

    // Canvas drawing state — auto-restore from persisted active pattern
    val drawnPoints = remember {
        mutableStateListOf<DrawnPoint>().also { list ->
            DrawnPatternUtils.getActiveDrawnPattern(context)?.let { (pts, _) ->
                list.addAll(pts)
            }
        }
    }
    var drawLoopEnabled by remember {
        mutableStateOf(
            DrawnPatternUtils.getActiveDrawnPattern(context)?.second ?: false
        )
    }

    // Drawn pattern save/load state
    var showDrawnSaveDialog by remember { mutableStateOf(false) }
    var showDrawnLoadDialog by remember { mutableStateOf(false) }
    var drawnSaveName by remember { mutableStateOf("") }
    var savedDrawnPatterns by remember { mutableStateOf(DrawnPatternUtils.loadDrawnPatterns(context)) }

    ScreenAnnouncement(S.accessibility.vibrationScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
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
            Text(S.vibration.predefinedEffects, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.sectionHeading())
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
        Text(S.vibration.customWaveform, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.sectionHeading())
        Text(
            "Amplitude: 0 = off, 1 = full\nDuration: ms  ·  Gap: pause between steps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // Speed presets
        Text(S.vibration.speedPresets, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(S.vibration.slow to 200f, S.vibration.medium to 80f, S.vibration.fast to 30f, S.vibration.rapid to 10f).forEach { (label, gap) ->
                FilterChip(
                    selected = gapMs == gap,
                    onClick  = { gapMs = gap },
                    label    = { Text(label) },
                )
            }
        }

        // Gap slider
        SliderWithInput(
            value = gapMs,
            onValueChange = { gapMs = it },
            valueRange = 0f..500f,
            formatValue = { "%.0f".format(it) },
            suffix = "ms",
            label = "Gap between steps: ${gapMs.toInt()} ms",
        )

        // Loop toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { },
        ) {
            Text(
                S.vibration.loopWaveform,
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
        Text(S.vibration.drawPattern, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.sectionHeading())
        Text(
            S.vibration.drawInstructions,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        val canvasBg = MaterialTheme.colorScheme.surfaceVariant
        val drawColor = MaterialTheme.colorScheme.primary
        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        val gridColor = Color.Gray.copy(alpha = 0.3f)

        AccessibleCanvas(
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
                        onDragEnd = {
                            DrawnPatternUtils.setActiveDrawnPattern(context, drawnPoints.toList(), drawLoopEnabled)
                        },
                    )
                },
            contentDescription = S.accessibility.vibrationCanvasDesc(),
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
            Text(S.vibration.msZero, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text(S.vibration.ms2000, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }

        // Draw loop toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { },
        ) {
            Text(
                "Loop drawn pattern",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = drawLoopEnabled, onCheckedChange = {
                drawLoopEnabled = it
                if (drawnPoints.isNotEmpty()) {
                    DrawnPatternUtils.setActiveDrawnPattern(context, drawnPoints.toList(), it)
                }
            })
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
                        DrawnPatternUtils.setActiveDrawnPattern(context, drawnPoints.toList(), drawLoopEnabled)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = drawnPoints.isNotEmpty(),
            ) { Text(S.vibration.playDrawn, maxLines = 1, softWrap = false) }

            OutlinedButton(
                onClick = {
                    drawnPoints.clear()
                    DrawnPatternUtils.clearActiveDrawnPattern(context)
                },
                modifier = Modifier.weight(1f),
            ) { Text(S.vibration.clearDrawing, maxLines = 1, softWrap = false) }
        }

        // ── Save / Load drawn patterns ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { showDrawnSaveDialog = true },
                modifier = Modifier.weight(1f),
                enabled = drawnPoints.isNotEmpty(),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.vibration.savePattern, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    savedDrawnPatterns = DrawnPatternUtils.loadDrawnPatterns(context)
                    showDrawnLoadDialog = true
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.vibration.loadPattern, maxLines = 1, softWrap = false)
            }
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
                                            Icon(Icons.Default.FileOpen, S.accessibility.loadPattern)
                                        }
                                        IconButton(onClick = {
                                            savedPatterns = savedPatterns.toMutableList().also { it.removeAt(idx) }
                                            savePatterns(context, savedPatterns)
                                        }) {
                                            Icon(Icons.Default.Delete, S.accessibility.deletePattern, tint = MaterialTheme.colorScheme.error)
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

    // ── Save drawn pattern dialog ─────────────────────────────────────────
    if (showDrawnSaveDialog) {
        AlertDialog(
            onDismissRequest = { showDrawnSaveDialog = false },
            title = { Text(S.vibration.savePattern) },
            text = {
                OutlinedTextField(
                    value = drawnSaveName,
                    onValueChange = { drawnSaveName = it },
                    label = { Text(S.vibration.patternName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (drawnSaveName.isNotBlank()) {
                            val pattern = SavedDrawnPattern(
                                name = drawnSaveName.trim(),
                                points = drawnPoints.toList(),
                                loop = drawLoopEnabled,
                            )
                            savedDrawnPatterns = (listOf(pattern) + savedDrawnPatterns).take(MAX_SAVED)
                            DrawnPatternUtils.saveDrawnPatterns(context, savedDrawnPatterns)
                            drawnSaveName = ""
                            showDrawnSaveDialog = false
                        }
                    },
                    enabled = drawnSaveName.isNotBlank(),
                ) { Text(S.vibration.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDrawnSaveDialog = false }) { Text(S.vibration.cancel) }
            },
        )
    }

    // ── Load drawn pattern dialog ─────────────────────────────────────────
    if (showDrawnLoadDialog) {
        AlertDialog(
            onDismissRequest = { showDrawnLoadDialog = false },
            title = { Text(S.vibration.loadPattern) },
            text = {
                if (savedDrawnPatterns.isEmpty()) {
                    Text(S.vibration.noSavedPatterns, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        savedDrawnPatterns.forEachIndexed { idx, p ->
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
                                            "${p.points.size} points${if (p.loop) " · loop" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            drawnPoints.clear()
                                            drawnPoints.addAll(p.points)
                                            drawLoopEnabled = p.loop
                                            DrawnPatternUtils.setActiveDrawnPattern(context, p.points, p.loop)
                                            showDrawnLoadDialog = false
                                        }) {
                                            Icon(Icons.Default.FileOpen, S.accessibility.loadPattern)
                                        }
                                        IconButton(onClick = {
                                            savedDrawnPatterns = savedDrawnPatterns.toMutableList().also { it.removeAt(idx) }
                                            DrawnPatternUtils.saveDrawnPatterns(context, savedDrawnPatterns)
                                        }) {
                                            Icon(Icons.Default.Delete, S.accessibility.deletePattern, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDrawnLoadDialog = false }) { Text(S.vibration.close) }
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
    val (timings, amplitudes) = DrawnPatternUtils.toWaveformArrays(points, hasAmplitude)
    val repeatIdx = if (loop) 0 else -1
    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeatIdx))
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
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp).minimumTouchTarget()) {
                        Icon(Icons.Default.Close, S.accessibility.removeStep, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (hasAmplitude) {
                SliderWithInput(
                    value = amplitude * 100f,
                    onValueChange = { onAmpChange(it / 100f) },
                    valueRange = 0f..100f,
                    formatValue = { "%.0f".format(it) },
                    suffix = "%",
                    label = "Amplitude: ${"%.0f".format(amplitude * 100)}%",
                )
            }

            SliderWithInput(
                value = duration,
                onValueChange = onDurChange,
                valueRange = 10f..2000f,
                formatValue = { "%.0f".format(it) },
                suffix = "ms",
                label = "Duration: ${duration.toInt()} ms",
            )
        }
    }
}
