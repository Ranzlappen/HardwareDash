package com.gadget.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.widget.DrawnPatternUtils
import com.gadget.widget.VibrationWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class VibrationService : Service() {

    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVibration()
            ACTION_STOP -> stopVibration()
        }
        return START_NOT_STICKY
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib

        if (!vib.hasVibrator()) {
            stopSelf()
            return
        }

        ensureChannel()
        val lang = LocalizationManager.loadLanguage(this)
        val notification = Notification.Builder(this, CH_VIBRATION)
            .setContentTitle(S.Services.vibrationActive(lang))
            .setContentText(S.Services.tapToStop(lang))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, VibrationService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)

        val hasAmplitude = vib.hasAmplitudeControl()
        val effect = try {
            val activePattern = DrawnPatternUtils.getActiveDrawnPattern(this)
            if (activePattern != null) {
                val (points, _) = activePattern
                val (t, a) = DrawnPatternUtils.toWaveformArrays(points, hasAmplitude)
                if (t.isEmpty()) error("empty pattern")
                VibrationEffect.createWaveform(t, a, 0)
            } else {
                null
            }
        } catch (e: Exception) { Timber.e(e, "Failed to parse drawn vibration pattern"); null }
            ?: VibrationEffect.createWaveform(
                longArrayOf(0, 500, 200, 500),
                intArrayOf(0, 255, 0, 255),
                0,
            )

        vib.vibrate(effect)
    }

    private fun stopVibration() {
        try { vibrator?.cancel() } catch (e: Exception) { Timber.w(e, "Failed to cancel vibrator during stop") }
        vibrator = null
        isRunning = false

        // Update widget state so it shows OFF
        val prefs = getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(WIDGET_KEY_VIBRATING, false).apply()

        // Refresh all vibration widgets to show OFF state
        refreshWidgets()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { vibrator?.cancel() } catch (e: Exception) { Timber.w(e, "Failed to cancel vibrator during destroy") }
        isRunning = false
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, VibrationWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val intent = Intent(this, VibrationWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_VIBRATION, "Vibration Service", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_START = "com.gadget.VIBRATION_START"
        const val ACTION_STOP = "com.gadget.VIBRATION_STOP"
        private const val CH_VIBRATION = "hwd_vibration"
        private const val NOTIF_ID = 7005
        private const val WIDGET_PREFS = "widget_vibration"
        private const val WIDGET_KEY_VIBRATING = "vibrating"

        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, VibrationService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                true
            } else {
                context.startService(Intent(context, VibrationService::class.java).apply { action = ACTION_STOP })
                isRunning = false
                false
            }
        }
    }
}
