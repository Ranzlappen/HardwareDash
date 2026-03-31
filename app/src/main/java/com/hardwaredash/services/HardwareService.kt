package com.hardwaredash.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hardwaredash.MainActivity

/**
 * Optional foreground Service for continuous background hardware access.
 *
 * WHAT IT GIVES YOU:
 *  - Keeps the app process alive when the user leaves the app
 *  - Declared with foregroundServiceType="camera|microphone" in the manifest
 *    so Android allows camera/mic access from the background
 *  - Shows a persistent (but silent) status bar notification
 *
 * HOW TO START from a Composable:
 *   val ctx = LocalContext.current
 *   val intent = Intent(ctx, HardwareService::class.java)
 *   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
 *       ctx.startForegroundService(intent)
 *   else
 *       ctx.startService(intent)
 *
 * HOW TO STOP:
 *   ctx.stopService(Intent(ctx, HardwareService::class.java))
 *
 * HOW TO COMMUNICATE (extend as needed):
 *  - Use a SharedFlow / StateFlow in a singleton object that both the
 *    Service and the Compose UI observe.
 *  - Or bind to the service and call methods directly.
 */
class HardwareService : Service() {

    companion object {
        private const val CHANNEL_ID = "hwd_foreground_channel"
        private const val NOTIF_ID   = 9001

        // Action strings for the notification buttons
        const val ACTION_STOP = "com.hardwaredash.ACTION_STOP"
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle action buttons from the notification
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // START_STICKY: if the system kills the service, restart it automatically
        return START_STICKY
    }

    override fun onDestroy() {
        // Release any hardware resources acquired by the service here
        super.onDestroy()
    }

    // Services don't need binding for this use-case
    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("HardwareDash")
        .setContentText("Hardware monitoring is running in the background.")
        .setOngoing(true)           // cannot be swiped away by the user
        .setSilent(true)            // no sound/vibration
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .addAction(
            android.R.drawable.ic_delete,
            "Stop",
            PendingIntent.getService(
                this, 1,
                Intent(this, HardwareService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HardwareDash Background Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description    = "Persistent notification while hardware monitoring is active."
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
