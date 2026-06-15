package dev.ranzlappen.gadget.feature.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecordService : Service() {

    @Inject
    lateinit var recorder: AudioRecorder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORD -> {
                promoteToForeground()
                recorder.startRecording()
            }
            ACTION_STOP_RECORD -> {
                recorder.stopAndSave()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (recorder.isRecording.value) recorder.stopAndSave()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.audio_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setSound(null, null); enableVibration(false); setShowBadge(false)
                }
            )
        }
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, AudioRecordService::class.java).setAction(ACTION_STOP_RECORD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.audio_notification_title))
            .setContentText(getString(R.string.audio_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, getString(R.string.audio_notification_stop), stopPi)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_START_RECORD = "dev.ranzlappen.gadget.feature.audio.ACTION_START_RECORD"
        const val ACTION_STOP_RECORD = "dev.ranzlappen.gadget.feature.audio.ACTION_STOP_RECORD"
        private const val NOTIFICATION_CHANNEL_ID = "audio_record"
        private const val NOTIFICATION_ID = 0x41_55_44_4F // "AUDO"
    }
}
