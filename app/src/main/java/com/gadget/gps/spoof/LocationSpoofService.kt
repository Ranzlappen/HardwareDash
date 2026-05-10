package com.gadget.gps.spoof

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gadget.MainActivity
import com.gadget.R
import com.gadget.ui.screens.notifications.CH_GPS_SPOOF
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that owns the wake-lock + notification while
 * GPX/KML/Route playback is running. The controller hosts the actual
 * emission loop; this service exists purely to keep the process out of Doze
 * and to surface a persistent stop affordance to the user.
 *
 * Static spoofing does NOT use this service — a single setTestProviderLocation
 * is enough.
 */
@AndroidEntryPoint
class LocationSpoofService : Service() {

    @Inject lateinit var controller: GpsSpoofController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { runCatching { controller.stop() } }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundCompat(buildNotification("Starting…", null))
        acquireWakeLock()

        observerJob?.cancel()
        observerJob = scope.launch {
            controller.state.collectLatest { state ->
                when (state) {
                    is SpoofState.Running -> {
                        val title = state.sourceLabel
                        val body = formatLatLon(state.currentLat, state.currentLon)
                        notify(buildNotification(title, body))
                    }
                    SpoofState.Idle -> stopSelf()
                    is SpoofState.Error -> stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notify(notification: Notification) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, body: String?): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, LocationSpoofService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CH_GPS_SPOOF)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body ?: "GPS spoofing active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            // Hard ceiling — controller's own session limit will stop the service first,
            // but if anything goes wrong, the OS will release the lock at this point.
            acquire(GpsSpoofController.DEFAULT_SESSION_LIMIT_MS + 30_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun formatLatLon(lat: Double, lon: Double): String =
        "%.5f, %.5f".format(lat, lon)

    companion object {
        const val NOTIFICATION_ID = 0xC0FFEE
        const val ACTION_STOP = "com.gadget.gps.spoof.STOP"
        private const val WAKE_LOCK_TAG = "HardwareDash:GpsSpoof"

        fun start(context: Context) {
            val intent = Intent(context, LocationSpoofService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationSpoofService::class.java))
        }
    }
}
