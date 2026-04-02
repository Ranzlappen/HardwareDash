// CHANGE: Dynamic N-step waveform builder, loop toggle, gap/frequency control, speed presets
// REASON: Make vibration actuator much more flexible and customizable
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            Text("Vibration Motor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (!hasAmplitude) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "⚠️  This device doesn't support amplitude control. Patterns will play at full strength.",
                    modifier = Modifier.padding(12.dp),
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        // ── Predefined patterns ───────────────────────────────────────────────
        if (patterns.isNotEmpty()) {
            Text("Predefined Haptic Effects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                patterns.forEach { p ->
                    ElevatedButton(onClick = {
                        vibrator.cancel()
                        vibrator.vibrate(p.effect())
                    }) { Text(p.label) }
                }
            }
        }

        HorizontalDivider()

        // ── Custom waveform ───────────────────────────────────────────────────
        Text("Custom Waveform Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Amplitude: 0 = off, 1 = full\nDuration: ms  ·  Gap: pause between steps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // Speed presets
        Text("Speed Presets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
                val timings    = mutableListOf<Long>()
                val amplitudes = mutableListOf<Int>()
                steps.forEachIndexed { idx, step ->
                    if (idx > 0) {
                        // Gap before this step
                        timings.add(gapMs.toLong())
                        amplitudes.add(0)
                    }
                    timings.add(step.duration.toLong())
                    amplitudes.add(
                        if (hasAmplitude) (step.amplitude * 255).toInt() else 255
                    )
                }
                val repeatIdx = if (loopEnabled) 0 else -1
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        timings.toLongArray(),
                        amplitudes.toIntArray(),
                        repeatIdx,
                    )
                )
            }
        ) { Text(if (loopEnabled) "▶  Play (Looping)" else "▶  Play Custom Waveform") }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onClick  = { vibrator.cancel() },
        ) { Text("⏹  Stop") }
    }
}

@Composable
private fun WaveformStep(
    label: String,
    amplitude: Float, duration: Float,
    hasAmplitude: Boolean,
    onAmpChange: (Float) -> Unit, onDurChange: (Float) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
