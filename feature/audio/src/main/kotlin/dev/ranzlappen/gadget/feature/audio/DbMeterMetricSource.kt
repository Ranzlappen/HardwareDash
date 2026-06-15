package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

@Singleton
class DbMeterMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Microphone level",
        unit = "dB",
        min = 0f,
        max = 60f,
        category = MetricCategory.Sensor,
    )

    override suspend fun sample(): Float = stream()?.first() ?: 0f

    override fun stream(): Flow<Float>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) return null
        return callbackFlow {
            val sampleRate = 44_100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
            if (minBuf <= 0) { close(); return@callbackFlow }
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, encoding, minBuf * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); close(); return@callbackFlow }
            record.startRecording()
            val buf = ShortArray(minBuf)
            try {
                while (!isClosedForSend) {
                    val read = record.read(buf, 0, buf.size)
                    if (read > 0) {
                        val rms = sqrt(buf.take(read).fold(0.0) { acc, s -> acc + s.toDouble() * s } / read)
                        val db = if (rms > 0) (20.0 * log10(rms / Short.MAX_VALUE) + 60.0).coerceIn(0.0, 60.0) else 0.0
                        trySend(db.toFloat())
                    }
                    delay(500)
                }
            } finally {
                record.stop()
                record.release()
            }
            awaitClose { }
        }
    }

    companion object {
        const val METRIC_KEY = "db_meter"
    }
}
