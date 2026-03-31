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
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun VibrationScreen() {
    val context  = LocalContext.current
    val vibrator = remember { getVibrator(context) }

    // Custom waveform sliders
    var amp1 by remember { mutableFloatStateOf(0.3f) }
    var dur1 by remember { mutableFloatStateOf(100f) }
    var amp2 by remember { mutableFloatStateOf(0.8f) }
    var dur2 by remember { mutableFloatStateOf(200f) }
    var amp3 by remember { mutableFloatStateOf(1.0f) }
    var dur3 by remember { mutableFloatStateOf(150f) }

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
        Text("Custom 3-Step Waveform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Amplitude: 0 = off, 1 = full\nDuration: ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        WaveformStep("Step 1", amp1, dur1, hasAmplitude, { amp1 = it }, { dur1 = it })
        WaveformStep("Step 2", amp2, dur2, hasAmplitude, { amp2 = it }, { dur2 = it })
        WaveformStep("Step 3", amp3, dur3, hasAmplitude, { amp3 = it }, { dur3 = it })

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick  = {
                vibrator.cancel()
                val timings    = longArrayOf(0, dur1.toLong(), 50, dur2.toLong(), 50, dur3.toLong())
                val amplitudes = intArrayOf(
                    0,
                    if (hasAmplitude) (amp1 * 255).toInt() else 255,
                    0,
                    if (hasAmplitude) (amp2 * 255).toInt() else 255,
                    0,
                    if (hasAmplitude) (amp3 * 255).toInt() else 255,
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        ) { Text("▶  Play Custom Waveform") }

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
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

            if (hasAmplitude) {
                Text("Amplitude: ${"%.0f".format(amplitude * 100)}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = amplitude, onValueChange = onAmpChange, valueRange = 0f..1f)
            }

            Text("Duration: ${duration.toInt()} ms", style = MaterialTheme.typography.bodySmall)
            Slider(value = duration, onValueChange = onDurChange, valueRange = 20f..500f)
        }
    }
}
