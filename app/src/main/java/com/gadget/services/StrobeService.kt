package com.gadget.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.*

class StrobeService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startStrobe()
            ACTION_STOP -> stopStrobe()
        }
        return START_NOT_STICKY
    }

    private fun startStrobe() {
        ensureChannel()
        val lang = LocalizationManager.loadLanguage(this)
        val notification = Notification.Builder(this, CH_STROBE)
            .setContentTitle(S.Services.strobeActive(lang))
            .setContentText(S.Services.tapToStop(lang))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, StrobeService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)

        job?.cancel()
        job = scope.launch {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cid = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return@launch

            val prefs = getSharedPreferences("strobe_settings", Context.MODE_PRIVATE)
            val freq = prefs.getFloat("strobe_freq_hz", 5f)
            val halfPeriodMs = (500f / freq).toLong().coerceAtLeast(10L)

            try {
                while (isActive) {
                    cm.setTorchMode(cid, true)
                    delay(halfPeriodMs)
                    cm.setTorchMode(cid, false)
                    delay(halfPeriodMs)
                }
            } finally {
                try { cm.setTorchMode(cid, false) } catch (_: Exception) {}
            }
        }
    }

    private fun stopStrobe() {
        job?.cancel()
        job = null
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
            NotificationChannel(CH_STROBE, "Strobe Service", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_START = "com.gadget.STROBE_START"
        const val ACTION_STOP = "com.gadget.STROBE_STOP"
        private const val CH_STROBE = "hwd_strobe"
        private const val NOTIF_ID = 7001

        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, StrobeService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                true
            } else {
                context.startService(Intent(context, StrobeService::class.java).apply { action = ACTION_STOP })
                isRunning = false
                false
            }
        }
    }
}
