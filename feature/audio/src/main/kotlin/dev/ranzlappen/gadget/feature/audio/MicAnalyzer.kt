package dev.ranzlappen.gadget.feature.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/** One analysed block of microphone audio. */
data class MicFrame(
    val db: Float,
    val bands: List<Float>,
    val peakHz: Float,
)

/**
 * Live microphone analysis for the Audio screen (v3 B10): one `AudioRecord`
 * read on the IO dispatcher, ~21 frames/s (2048 samples at 44.1 kHz), each
 * with the dB level, [BAND_COUNT] spectrum bands and the dominant frequency.
 *
 * Callers check RECORD_AUDIO first; the flow completes empty if the mic
 * can't be opened (busy, denied, emulator without input).
 */
class MicAnalyzer @Inject constructor() {

    @SuppressLint("MissingPermission") // caller gates on RECORD_AUDIO
    fun frames(): Flow<MicFrame> = flow {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuf <= 0) return@flow
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuf, FFT_SIZE * 2) * 2)
        } catch (_: SecurityException) {
            return@flow
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@flow
        }
        val block = ShortArray(FFT_SIZE)
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < FFT_SIZE) {
                    val read = record.read(block, filled, FFT_SIZE - filled)
                    if (read <= 0) return@flow
                    filled += read
                }
                val mags = SpectrumMath.magnitudes(block, FFT_SIZE)
                emit(
                    MicFrame(
                        db = SpectrumMath.rmsDb(block),
                        bands = SpectrumMath.bands(mags, SAMPLE_RATE, BAND_COUNT, MIN_HZ, MAX_HZ),
                        peakHz = SpectrumMath.peakHz(mags, SAMPLE_RATE),
                    ),
                )
            }
        } finally {
            try { record.stop() } catch (_: IllegalStateException) {}
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 44_100
        const val FFT_SIZE = 2048
        const val BAND_COUNT = 32
        const val MIN_HZ = 40f
        const val MAX_HZ = 16_000f
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
