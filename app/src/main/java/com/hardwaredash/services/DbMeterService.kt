package com.hardwaredash.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import com.hardwaredash.R
import com.hardwaredash.localization.LocalizationManager
import com.hardwaredash.localization.S
import com.hardwaredash.widget.DbMeterWidgetProvider
import kotlinx.coroutines.*
import kotlin.math.*

class DbMeterService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (job != null) return
        ensureChannel()
        val lang = LocalizationManager.loadLanguage(this)
        val notification = Notification.Builder(this, CH_DB)
            .setContentTitle(S.Services.dbMeterActive(lang))
            .setContentText(S.Services.monitoringMicLevel(lang))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, DbMeterService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)
        isRunning = true

        job = scope.launch {
            val bufSize = AudioRecord.getMinBufferSize(
                44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, 44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                stopSelf()
                return@launch
            }

            val buffer = ShortArray(1024)
            recorder.startRecording()

            try {
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val rms = sqrt(buffer.take(read).map { it.toDouble().pow(2) }.average())
                        val dbFs = if (rms > 0) (20 * log10(rms / Short.MAX_VALUE)).toFloat() else -60f
                        val db = (60f + dbFs).coerceIn(0f, 60f)
                        updateWidgets("${"%.1f".format(db)} dB")
                    }
                    delay(500L)
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
        isRunning = false
        updateWidgets("-- dB")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateWidgets(value: String) {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, DbMeterWidgetProvider::class.java))
        for (id in ids) {
            val views = RemoteViews(packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_btn_speak_now)
            views.setTextViewText(R.id.widget_action_label, value)
            manager.updateAppWidget(id, views)
        }
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
            NotificationChannel(CH_DB, "dB Meter", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_START = "com.hardwaredash.DB_METER_START"
        const val ACTION_STOP = "com.hardwaredash.DB_METER_STOP"
        private const val CH_DB = "hwd_db_meter"
        private const val NOTIF_ID = 7004
        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, DbMeterService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
                true
            } else {
                context.startService(Intent(context, DbMeterService::class.java).apply { action = ACTION_STOP })
                false
            }
        }
    }
}
