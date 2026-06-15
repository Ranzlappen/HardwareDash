package dev.ranzlappen.gadget.feature.audio

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val pcmBuffer = java.io.ByteArrayOutputStream()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun startRecording() {
        if (_isRecording.value) return
        val sampleRate = 44_100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuf <= 0) return
        val bufSize = minBuf * 4

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            encoding,
            bufSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        audioRecord = record
        pcmBuffer.reset()
        _isRecording.value = true
        captureJob = scope.launch {
            val buf = ByteArray(bufSize)
            record.startRecording()
            while (isActive) {
                val read = record.read(buf, 0, bufSize)
                if (read > 0) pcmBuffer.write(buf, 0, read)
            }
        }
    }

    fun stopAndSave(): Uri? {
        captureJob?.cancel()
        captureJob = null
        val record = audioRecord ?: return null
        try { record.stop() } catch (_: Exception) {}
        record.release()
        audioRecord = null
        _isRecording.value = false

        val pcmData = pcmBuffer.toByteArray()
        pcmBuffer.reset()
        if (pcmData.isEmpty()) return null

        val wavData = buildWav(pcmData, sampleRate = 44_100, channels = 1, bitsPerSample = 16)
        return saveToMediaStore(wavData)
    }

    private fun buildWav(pcm: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val fileSize = 36 + dataSize
        return ByteArray(44 + dataSize).also { wav ->
            fun writeInt(offset: Int, v: Int) {
                wav[offset] = (v and 0xFF).toByte(); wav[offset+1] = (v shr 8 and 0xFF).toByte()
                wav[offset+2] = (v shr 16 and 0xFF).toByte(); wav[offset+3] = (v shr 24 and 0xFF).toByte()
            }
            fun writeShort(offset: Int, v: Int) {
                wav[offset] = (v and 0xFF).toByte(); wav[offset+1] = (v shr 8 and 0xFF).toByte()
            }
            "RIFF".toByteArray().copyInto(wav, 0)
            writeInt(4, fileSize)
            "WAVE".toByteArray().copyInto(wav, 8)
            "fmt ".toByteArray().copyInto(wav, 12)
            writeInt(16, 16)
            writeShort(20, 1) // PCM
            writeShort(22, channels)
            writeInt(24, sampleRate)
            writeInt(28, byteRate)
            writeShort(32, blockAlign)
            writeShort(34, bitsPerSample)
            "data".toByteArray().copyInto(wav, 36)
            writeInt(40, dataSize)
            pcm.copyInto(wav, 44)
        }
    }

    private fun saveToMediaStore(wavData: ByteArray): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "recording_${System.currentTimeMillis()}.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Gadget")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri)?.use { it.write(wavData) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        return uri
    }
}
