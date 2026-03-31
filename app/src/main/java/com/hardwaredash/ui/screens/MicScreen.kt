package com.hardwaredash.ui.screens

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import kotlinx.coroutines.*
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MicScreen() {
    val perm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    when {
        perm.status.isGranted -> MicMeter()
        perm.status.shouldShowRationale -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Microphone access is needed to show live audio levels.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { perm.launchPermissionRequest() }) { Text("Grant Permission") }
        }
        else -> LaunchedEffect(Unit) { perm.launchPermissionRequest() }
    }
}

@Composable
private fun MicMeter() {
    var isRecording  by remember { mutableStateOf(false) }
    var amplitude    by remember { mutableFloatStateOf(0f) }       // 0..1 normalised
    var peakDb       by remember { mutableFloatStateOf(-60f) }     // dB
    var history      by remember { mutableStateOf(List(60) { 0f }) }

    // ── AudioRecord loop in coroutine ──────────────────────────────────────
    LaunchedEffect(isRecording) {
        if (!isRecording) { amplitude = 0f; return@LaunchedEffect }

        val sampleRate  = 44100
        val bufferSize  = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return@LaunchedEffect

        val buffer = ShortArray(bufferSize / 2)
        recorder.startRecording()

        try {
            while (isRecording) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    // RMS amplitude
                    val rms  = sqrt(buffer.take(read).map { it.toDouble().pow(2) }.average())
                    val norm = (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
                    val db   = if (rms > 0) (20 * log10(rms / Short.MAX_VALUE)).toFloat()
                               else         -60f
                    amplitude = norm
                    peakDb    = db
                    history   = (history.drop(1) + norm)
                }
                delay(16L) // ~60 fps
            }
        } finally {
            recorder.stop()
            recorder.release()
            amplitude = 0f
        }
    }

    DisposableEffect(Unit) { onDispose { isRecording = false } }

    // Animated smooth amplitude for the meter arc
    val animatedAmp by animateFloatAsState(
        targetValue   = amplitude,
        animationSpec = tween(80),
        label         = "amp"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (isRecording) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("Microphone Meter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // ── Arc meter ──────────────────────────────────────────────────────
        val meterColor  = MaterialTheme.colorScheme.primary
        val peakColor   = MaterialTheme.colorScheme.error
        val trackColor  = MaterialTheme.colorScheme.surfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val stroke  = 28.dp.toPx()
            val padding = stroke / 2 + 8.dp.toPx()
            val arcSize = Size(size.width - padding * 2, (size.width - padding * 2))
            val topLeft = Offset(padding, size.height - arcSize.height / 2 - padding)

            // Track
            drawArc(trackColor, 180f, 180f, false,
                topLeft = topLeft, size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))

            // Level arc
            val sweepAngle = animatedAmp * 180f
            val arcCol = when {
                animatedAmp > 0.85f -> peakColor
                animatedAmp > 0.6f  -> Color(0xFFFFC107)
                else                -> meterColor
            }
            drawArc(arcCol, 180f, sweepAngle, false,
                topLeft = topLeft, size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
        }

        Text(
            "${"%.1f".format(peakDb)} dBFS",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (peakDb > -6f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
        )

        // ── Waveform history bars ──────────────────────────────────────────
        val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val barW = size.width / history.size
            history.forEachIndexed { i, v ->
                val barH = v * size.height
                drawRect(
                    color   = barColor,
                    topLeft = Offset(i * barW, size.height - barH),
                    size    = Size(barW - 2.dp.toPx(), barH),
                )
            }
        }

        // ── Control button ─────────────────────────────────────────────────
        Button(
            onClick  = { isRecording = !isRecording },
            modifier = Modifier.fillMaxWidth(0.6f).height(52.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                 else             MaterialTheme.colorScheme.primary
            ),
        ) {
            Text(if (isRecording) "⏹  Stop" else "⏺  Start Monitoring")
        }

        Text(
            "Audio is processed locally — nothing is saved or transmitted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
