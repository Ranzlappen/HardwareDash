package com.gadget.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.widget.WidgetActionHandler
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceRecordService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pcmBuffer = ByteArrayOutputStream()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopAndSave()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        ensureChannel()
        val lang = LocalizationManager.loadLanguage(this)
        val notification = Notification.Builder(this, CH_VOICE)
            .setContentTitle(S.Services.recordingAudio(lang))
            .setContentText(S.Services.tapToStopSave(lang))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, VoiceRecordService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)

        pcmBuffer.reset()
        job?.cancel()
        job = scope.launch {
            val bufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                WidgetActionHandler.showToast(this@VoiceRecordService, S.Services.micInitFailed(LocalizationManager.loadLanguage(this@VoiceRecordService)))
                stopSelf()
                return@launch
            }

            val buffer = ShortArray(1024)
            recorder.startRecording()
            isRunning = true

            try {
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val bb = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) bb.putShort(buffer[i])
                        synchronized(pcmBuffer) { pcmBuffer.write(bb.array()) }
                    }
                    delay(10L)
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    private fun stopAndSave() {
        job?.cancel()
        job = null
        isRunning = false

        val pcmData = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
        pcmBuffer.reset()

        if (pcmData.isNotEmpty()) {
            val filename = "HWD_Rec_${System.currentTimeMillis()}.wav"
            val wavBytes = createWav(pcmData)
            val cv = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Gadget")
            }
            val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { it.write(wavBytes) }
                val lang2 = LocalizationManager.loadLanguage(this)
                WidgetActionHandler.showToast(this, S.Services.savedFile(lang2, filename))
            }
        } else {
            val lang2 = LocalizationManager.loadLanguage(this)
            WidgetActionHandler.showToast(this, S.Services.nothingRecorded(lang2))
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.cancel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_VOICE, "Voice Recording", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun createWav(pcmData: ByteArray): ByteArray {
        val byteRate = SAMPLE_RATE * 1 * 16 / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcmData.size)
            put("WAVE".toByteArray()); put("fmt ".toByteArray())
            putInt(16); putShort(1); putShort(1)
            putInt(SAMPLE_RATE); putInt(byteRate); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(pcmData.size)
        }
        return header.array() + pcmData
    }

    companion object {
        const val ACTION_START = "com.gadget.VOICE_REC_START"
        const val ACTION_STOP = "com.gadget.VOICE_REC_STOP"
        private const val CH_VOICE = "hwd_voice_rec"
        private const val NOTIF_ID = 7003
        private const val SAMPLE_RATE = 44100
        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, VoiceRecordService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
                true
            } else {
                context.startService(Intent(context, VoiceRecordService::class.java).apply { action = ACTION_STOP })
                false
            }
        }
    }
}
