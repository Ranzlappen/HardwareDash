package dev.ranzlappen.gadget.feature.keepalive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import dev.ranzlappen.gadget.feature.keepalive.R

/**
 * Shared (both flavors) foreground service that keeps in-process
 * background work (rule evaluators, widget pollers, scheduled actions)
 * alive across Doze and idle. Started by
 * `KeepAliveController.enable()`; stopped by `disable()` /
 * `disableAndStopService()`.
 *
 * The notification is `setOngoing(true)`, posted on a high-importance
 * channel, and uses `CATEGORY_SERVICE`. The content-tap intent resolves
 * the app's own launcher activity (via PackageManager) so this module
 * doesn't depend on `:app`'s MainActivity.
 */
class PersistentKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startForegroundLifeline()
        }
        return START_STICKY
    }

    private fun startForegroundLifeline() {
        ensureChannel()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val tapIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PersistentKeepAliveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CH_KEEPALIVE)
            .setContentTitle(getString(R.string.keepalive_notification_title))
            .setContentText(getString(R.string.keepalive_notification_body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .apply { if (tapIntent != null) setContentIntent(tapIntent) }
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.keepalive_notification_stop),
                    stopIntent,
                ).build(),
            )
            .also { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setForegroundServiceBehavior(
                        Notification.FOREGROUND_SERVICE_IMMEDIATE,
                    )
                }
            }
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH_KEEPALIVE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_KEEPALIVE,
                    "Keep Alive",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Persistent foreground lifeline"
                    setShowBadge(false)
                },
            )
        }
    }

    companion object {
        const val CH_KEEPALIVE: String = "keep_alive"
        const val NOTIF_ID: Int = 0xCAFE_E1
        const val ACTION_STOP: String = "dev.ranzlappen.gadget.action.KEEPALIVE_STOP"
    }
}
