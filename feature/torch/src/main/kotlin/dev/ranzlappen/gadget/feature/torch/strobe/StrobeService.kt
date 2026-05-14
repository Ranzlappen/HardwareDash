package dev.ranzlappen.gadget.feature.torch.strobe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that loops `TorchController.setOn(true/false)`
 * at a fixed 5 Hz cadence (100 ms on, 100 ms off) until told to
 * stop.
 *
 * Foreground-service contract:
 * - `foregroundServiceType="camera"` in the manifest so the OS
 *   permits flash access while in foreground state on API 34+.
 * - Posts a notification on a dedicated low-importance channel so
 *   the OS shows it without a sound / vibration interruption.
 * - The notification's only action is "tap the widget to stop" —
 *   we deliberately don't add a "stop" action button to keep the
 *   widget the canonical control surface (one toggle, one tap).
 *
 * Strobe rate is fixed for Phase 2 / Batch 1. Configurable rate is
 * a follow-up batch (needs a settings section + persistence).
 *
 * Strobe runs on a dedicated `Dispatchers.Default` coroutine so the
 * 100 ms `delay` doesn't stall the main thread. The torch
 * controller's `setOn` is thread-safe (Camera2 binder calls).
 */
@AndroidEntryPoint
class StrobeService : Service() {

    @Inject
    lateinit var torchController: TorchController

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var strobeLoop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopStrobing()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startStrobing()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // Belt-and-braces: cancel the loop and turn the torch off
        // synchronously even if onStartCommand(STOP) didn't fire
        // (system might kill the service for other reasons).
        // setOn is non-suspend so we can call it directly here
        // without launching into a coroutine that the cancelling
        // serviceJob would race.
        stopStrobing()
        torchController.setOn(false)
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startStrobing() {
        promoteToForeground()
        if (strobeLoop?.isActive == true) return
        strobeLoop = serviceScope.launch {
            // Coroutine guard — `while (isActive)` would also work
            // but the explicit cancellation path is clearer.
            var on = false
            while (true) {
                on = !on
                torchController.setOn(on)
                delay(STROBE_HALF_PERIOD_MS)
            }
        }
    }

    private fun stopStrobing() {
        strobeLoop?.cancel()
        strobeLoop = null
        // Final off — synchronous, no coroutine needed.
        torchController.setOn(false)
    }

    private fun promoteToForeground() {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.strobe_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.strobe_notification_title))
            .setContentText(getString(R.string.strobe_notification_text))
            .setSmallIcon(R.drawable.ic_strobe)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    companion object {
        /** Notification channel ID — stable across app versions. */
        const val NOTIFICATION_CHANNEL_ID = "strobe_service"

        /** Sticky notification ID for the foreground promotion. */
        const val NOTIFICATION_ID = 0x57_4F_5F_53 // "WO_S" ascii prefix; unique inside this app.

        /**
         * Half-period of the strobe in milliseconds. 100 ms on + 100
         * ms off = 5 Hz, well below the Camera2 rate-limit cliff on
         * most OEMs (most rate-limit around 8–10 Hz).
         */
        const val STROBE_HALF_PERIOD_MS = 100L

        /** Intent action: stop the strobe + foreground service. */
        const val ACTION_STOP = "dev.ranzlappen.gadget.feature.torch.STROBE_STOP"
    }
}
