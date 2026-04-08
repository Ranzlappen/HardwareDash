// CHANGE: Added FFT-based real-time spectrum analyzer with frequency (Hz) visualization
// REASON: Add live dB and frequency graph visualization (spectrum analyzer style)
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.content.ContentValues
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import com.hardwaredash.localization.S
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

// ─── Radix-2 Cooley-Tukey FFT ────────────────────────────────────────────────
// Operates on real input, returns magnitude spectrum (first half only)
private fun fftMagnitude(input: FloatArray): FloatArray {
    val n = input.size
    // Ensure power of 2
    if (n and (n - 1) != 0) return FloatArray(0)

    // Bit-reversal permutation
    val real = input.copyOf()
    val imag = FloatArray(n)
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            real[i] = real[j].also { real[j] = real[i] }
            imag[i] = imag[j].also { imag[j] = imag[i] }
        }
    }

    // FFT butterfly
    var len = 2
    while (len <= n) {
        val halfLen = len / 2
        val angle = -2.0 * PI / len
        for (i in 0 until n step len) {
            for (k in 0 until halfLen) {
                val theta = angle * k
                val cosT = cos(theta).toFloat()
                val sinT = sin(theta).toFloat()
                val tReal = real[i + k + halfLen] * cosT - imag[i + k + halfLen] * sinT
                val tImag = real[i + k + halfLen] * sinT + imag[i + k + halfLen] * cosT
                real[i + k + halfLen] = real[i + k] - tReal
                imag[i + k + halfLen] = imag[i + k] - tImag
                real[i + k] += tReal
                imag[i + k] += tImag
            }
        }
        len = len shl 1
    }

    // Magnitude of first half (Nyquist)
    val halfN = n / 2
    val mag = FloatArray(halfN)
    for (i in 0 until halfN) {
        mag[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
    }
    return mag
}

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
            Text(S.mic.micPermRationale)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { perm.launchPermissionRequest() }) { Text(S.mic.grantPermission) }
        }
        else -> LaunchedEffect(Unit) { perm.launchPermissionRequest() }
    }
}

private const val FFT_SIZE = 1024
private const val SAMPLE_RATE = 44100

@Composable
private fun MicMeter() {
    val context = LocalContext.current
    var isRecording  by remember { mutableStateOf(false) }
    var amplitude    by remember { mutableFloatStateOf(0f) }       // 0..1 normalised
    var peakDb       by remember { mutableFloatStateOf(-60f) }     // dB
    var history      by remember { mutableStateOf(List(60) { 0f }) }
    var spectrum     by remember { mutableStateOf(FloatArray(FFT_SIZE / 2)) }
    var peakFreqHz   by remember { mutableFloatStateOf(0f) }

    // Recording state
    var isSaving     by remember { mutableStateOf(false) }
    var recStartTime by remember { mutableLongStateOf(0L) }
    var recElapsed   by remember { mutableLongStateOf(0L) }
    val pcmBuffer    = remember { ByteArrayOutputStream() }
    var savedFiles   by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }
    var playingUri   by remember { mutableStateOf<Uri?>(null) }
    val mediaPlayer  = remember { MediaPlayer() }

    // ── AudioRecord loop in coroutine ──────────────────────────────────────
    LaunchedEffect(isRecording) {
        if (!isRecording) { amplitude = 0f; return@LaunchedEffect }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(FFT_SIZE * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return@LaunchedEffect

        val buffer = ShortArray(FFT_SIZE)
        recorder.startRecording()

        try {
            while (isRecording) {
                val read = recorder.read(buffer, 0, FFT_SIZE)
                if (read > 0) {
                    // RMS amplitude
                    val rms  = sqrt(buffer.take(read).map { it.toDouble().pow(2) }.average())
                    val norm = (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
                    val dbFs = if (rms > 0) (20 * log10(rms / Short.MAX_VALUE)).toFloat()
                               else         -60f
                    val db   = (60f + dbFs).coerceIn(0f, 60f)
                    amplitude = norm
                    peakDb    = db
                    history   = (history.drop(1) + norm)

                    // Write PCM data to buffer if saving
                    if (isSaving) {
                        val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) byteBuffer.putShort(buffer[i])
                        pcmBuffer.write(byteBuffer.array())
                        recElapsed = System.currentTimeMillis() - recStartTime
                    }

                    // FFT spectrum
                    if (read == FFT_SIZE) {
                        val fftInput = FloatArray(FFT_SIZE) { i ->
                            buffer[i].toFloat() / Short.MAX_VALUE
                        }
                        val mag = fftMagnitude(fftInput)
                        if (mag.isNotEmpty()) {
                            spectrum = mag
                            var maxMag = 0f
                            var maxIdx = 1
                            for (i in 1 until mag.size) {
                                if (mag[i] > maxMag) {
                                    maxMag = mag[i]
                                    maxIdx = i
                                }
                            }
                            peakFreqHz = maxIdx * SAMPLE_RATE.toFloat() / FFT_SIZE
                        }
                    }
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (isRecording) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(S.mic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // ── Arc meter ──────────────────────────────────────────────────────
        val meterColor  = MaterialTheme.colorScheme.primary
        val peakColor   = MaterialTheme.colorScheme.error
        val trackColor  = MaterialTheme.colorScheme.surfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(MaterialTheme.shapes.medium)
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
            "${"%.1f".format(peakDb)} dB",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (peakDb > 54f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
        )

        // ── Waveform history bars ──────────────────────────────────────────
        val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium)) {
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

        HorizontalDivider()

        // ── Spectrum Analyzer ──────────────────────────────────────────────
        Text(
            S.mic.spectrumAnalyzer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (isRecording && peakFreqHz > 0) {
            Text(
                "Peak: ${"%.0f".format(peakFreqHz)} Hz",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        val specColor   = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        val specBgColor = MaterialTheme.colorScheme.surfaceVariant

        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(2.5f).clip(MaterialTheme.shapes.medium)) {
            drawRect(specBgColor, size = size)

            if (!isRecording || spectrum.isEmpty()) return@Canvas

            // Show up to ~10kHz (bin index = freq * fftSize / sampleRate)
            val maxBin = minOf(spectrum.size, (10000 * FFT_SIZE / SAMPLE_RATE))
            if (maxBin <= 1) return@Canvas

            val barW = size.width / maxBin
            // Convert to dB, clamp range -80..0
            var maxDb = -80f
            for (i in 1 until maxBin) {
                val magDb = if (spectrum[i] > 0) (20 * log10(spectrum[i].toDouble())).toFloat() else -80f
                if (magDb > maxDb) maxDb = magDb
            }
            val dbFloor = -80f
            val dbRange = (maxOf(maxDb, -10f) - dbFloor).coerceAtLeast(1f)

            for (i in 1 until maxBin) {
                val magDb = if (spectrum[i] > 0) (20 * log10(spectrum[i].toDouble())).toFloat() else -80f
                val normH = ((magDb - dbFloor) / dbRange).coerceIn(0f, 1f)
                val barH  = normH * size.height
                drawRect(
                    color   = specColor,
                    topLeft = Offset((i - 1) * barW, size.height - barH),
                    size    = Size(barW.coerceAtLeast(1f), barH),
                )
            }

            // Frequency labels
            val labelFreqs = listOf(100, 500, 1000, 2000, 5000, 10000)
            labelFreqs.forEach { freq ->
                val bin = freq * FFT_SIZE / SAMPLE_RATE
                if (bin in 1 until maxBin) {
                    val x = (bin - 1) * barW
                    drawLine(
                        Color.Gray.copy(alpha = 0.3f),
                        Offset(x, 0f), Offset(x, size.height),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                }
            }
        }

        Text(
            "0 Hz                                                           10 kHz",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        // ── Control button ─────────────────────────────────────────────────
        Button(
            onClick  = { isRecording = !isRecording },
            shape    = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(0.6f).height(52.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                 else             MaterialTheme.colorScheme.primary
            ),
        ) {
            Text(if (isRecording) S.mic.stopMonitor else S.mic.startMonitoring)
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // Recording Section
        // ══════════════════════════════════════════════════════════════════════
        Text(S.mic.audioRecording, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (isSaving) {
            val mins = (recElapsed / 60000).toInt()
            val secs = ((recElapsed % 60000) / 1000).toInt()
            Text(
                "Recording: %02d:%02d".format(mins, secs),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            // Record / Stop
            Button(
                onClick = {
                    if (!isSaving) {
                        // Start recording
                        if (!isRecording) isRecording = true // ensure monitor is running
                        pcmBuffer.reset()
                        recStartTime = System.currentTimeMillis()
                        recElapsed = 0L
                        isSaving = true
                    } else {
                        // Stop recording and save
                        isSaving = false
                        val pcmData = pcmBuffer.toByteArray()
                        pcmBuffer.reset()
                        if (pcmData.isNotEmpty()) {
                            val filename = "HWD_Rec_${System.currentTimeMillis()}.wav"
                            val wavBytes = createWavFile(pcmData, SAMPLE_RATE, 1, 16)
                            val cv = ContentValues().apply {
                                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/HardwareDash")
                            }
                            val uri = context.contentResolver.insert(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv
                            )
                            if (uri != null) {
                                context.contentResolver.openOutputStream(uri)?.use { it.write(wavBytes) }
                                savedFiles = listOf(filename to uri) + savedFiles
                                Toast.makeText(context, "Saved: $filename", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaving) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(if (isSaving) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isSaving) "Stop & Save" else "Record")
            }
        }

        Text(
            "System-level call recording (phone, WhatsApp, Discord) requires " +
            "privileged permissions not available to third-party apps on Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )

        // Saved recordings list
        if (savedFiles.isNotEmpty()) {
            Text(S.mic.savedRecordings, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            savedFiles.forEach { (name, uri) ->
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.AudioFile, null, modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            try {
                                if (playingUri == uri) {
                                    mediaPlayer.stop()
                                    mediaPlayer.reset()
                                    playingUri = null
                                } else {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(context, uri)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                    playingUri = uri
                                    mediaPlayer.setOnCompletionListener {
                                        playingUri = null
                                        mediaPlayer.reset()
                                    }
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, S.mic.playbackError, Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (playingUri == uri) Icons.Default.Stop else Icons.Default.PlayArrow,
                                "Play/Stop",
                                tint = if (playingUri == uri) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    // Cleanup media player
    DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }
}

/** Create a WAV file from raw PCM data. */
private fun createWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val dataSize = pcmData.size
    val fileSize = 36 + dataSize

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        // RIFF header
        put("RIFF".toByteArray())
        putInt(fileSize)
        put("WAVE".toByteArray())
        // fmt sub-chunk
        put("fmt ".toByteArray())
        putInt(16) // sub-chunk size
        putShort(1) // PCM format
        putShort(channels.toShort())
        putInt(sampleRate)
        putInt(byteRate)
        putShort(blockAlign.toShort())
        putShort(bitsPerSample.toShort())
        // data sub-chunk
        put("data".toByteArray())
        putInt(dataSize)
    }

    return header.array() + pcmData
}
