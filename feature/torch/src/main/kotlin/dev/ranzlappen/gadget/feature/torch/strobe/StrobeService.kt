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
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Foreground service that strobes the torch at a configurable rate.
 *
 * Per-tap configuration arrives via Intent extras when the strobe
 * widget starts the service:
 * - [EXTRA_RATE_HZ] — strobe rate in Hz (clamped to
 *   `TorchWidgetConfig.MIN_RATE_HZ..MAX_RATE_HZ`). Defaults to
 *   `TorchWidgetConfig.DEFAULT_RATE_HZ` if absent.
 * - [EXTRA_SOS_MODE] — boolean flag for SOS pattern playback. The
 *   flag is plumbed through end-to-end but the SOS pattern logic
 *   itself is deferred — today this value is read but the loop
 *   still emits a constant strobe regardless. Tracked at
 *   https://github.com/Ranzlappen/HardwareDash/issues/96.
 *
 * Lifecycle:
 * - [isRunning] companion-level flag flips `true` once the
 *   foreground notification has been posted, and `false` in
 *   [stopStrobing] / [onDestroy]. [StrobeWidgetProvider] reads it
 *   to decide whether a widget tap starts a new run or stops the
 *   existing one. The flag is a heuristic — if the OS kills the
 *   service without notifying us, the flag stays stale until the
 *   next user tap, which goes through `startForegroundService` →
 *   `onStartCommand` and re-syncs.
 *
 * Foreground-service contract:
 * - `foregroundServiceType="camera"` in the manifest so the OS
 *   permits flash access while in foreground state on API 34+.
 * - Posts a notification on a dedicated low-importance channel so
 *   the OS shows it without a sound / vibration interruption.
 *
 * Strobe runs on a dedicated `Dispatchers.Default` coroutine so the
 * `delay` calls don't stall the main thread. The torch controller's
 * `setOn` is thread-safe (Camera2 binder calls).
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
            else -> {
                val rateHz = (intent?.getFloatExtra(EXTRA_RATE_HZ, TorchWidgetConfig.DEFAULT_RATE_HZ)
                    ?: TorchWidgetConfig.DEFAULT_RATE_HZ)
                    .coerceIn(TorchWidgetConfig.MIN_RATE_HZ, TorchWidgetConfig.MAX_RATE_HZ)
                @Suppress("UNUSED_VARIABLE")
                val sosMode = intent?.getBooleanExtra(EXTRA_SOS_MODE, false) ?: false
                // sosMode is plumbed but its playback pattern is
                // deferred — see issue #96. Today the constant
                // strobe runs at `rateHz` regardless of the flag.
                startStrobing(rateHz)
            }
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

    private fun startStrobing(rateHz: Float) {
        promoteToForeground()
        // Re-tap while running = no-op (the running loop already
        // honours the current rate; future "edit a live strobe"
        // UX can replace the loop without stopping by cancelling
        // the existing strobeLoop and relaunching).
        if (strobeLoop?.isActive == true) return
        isRunning = true
        val halfPeriodMs = halfPeriodMillis(rateHz)
        strobeLoop = serviceScope.launch {
            var on = false
            while (true) {
                on = !on
                torchController.setOn(on)
                delay(halfPeriodMs)
            }
        }
    }

    private fun stopStrobing() {
        strobeLoop?.cancel()
        strobeLoop = null
        // Final off — synchronous, no coroutine needed.
        torchController.setOn(false)
        isRunning = false
    }

    private fun promoteToForeground() {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: shortService — no CAMERA permission required,
            // OS enforces a ~3 min cap (acceptable safety bound for a
            // strobe session). Matches the manifest's declared type.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            // API 29-33: promote without a type. setTorchMode flash
            // access needs no camera-typed FGS here, and the manifest
            // declares only `shortService` (an unrecognised flag pre-34,
            // so the declared set is effectively empty) — passing
            // FOREGROUND_SERVICE_TYPE_CAMERA would not be a subset of
            // the declared types and throws IllegalArgumentException.
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Called by the OS on API 34+ when the shortService timeout
     * elapses (~3 min by spec). Stop strobing + tear down the
     * service rather than letting Android force-kill us — a clean
     * stop means widget state flips back to "off" via the
     * StrobeService.isRunning flag.
     */
    override fun onTimeout(startId: Int) {
        stopStrobing()
        stopSelf(startId)
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

        /** Intent action: stop the strobe + foreground service. */
        const val ACTION_STOP = "dev.ranzlappen.gadget.feature.torch.STROBE_STOP"

        /** Float extra (Hz) carrying the strobe rate from the
         *  widget's stored config. Read in [onStartCommand]. */
        const val EXTRA_RATE_HZ = "dev.ranzlappen.gadget.feature.torch.EXTRA_RATE_HZ"

        /** Boolean extra carrying the SOS toggle. Plumbed but the
         *  pattern logic is deferred — see issue #96. */
        const val EXTRA_SOS_MODE = "dev.ranzlappen.gadget.feature.torch.EXTRA_SOS_MODE"

        /**
         * Heuristic flag indicating whether a strobe loop is
         * currently running.
         *
         * Set `true` once the foreground notification is posted and
         * the loop launches; set `false` in [stopStrobing] /
         * [onDestroy]. Read by [dev.ranzlappen.gadget.feature.torch.widget.StrobeWidgetProvider]
         * to branch between "start" and "stop" intents on a widget
         * tap.
         *
         * `@Volatile` so cross-thread reads from the widget
         * provider's `onReceive` (broadcast receiver thread) see
         * the most recent write from the service.
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * Compute the half-period (ms) for a given Hz value. A
         * full strobe cycle is one ON half + one OFF half, so the
         * service `delay(halfPeriodMs)` between flips:
         * `halfPeriodMs = 500 / rateHz` (rounded).
         *
         * Clamped to at least 25 ms to avoid the OEM camera-rate
         * cliff at very high Hz; the widget UI caps at 20 Hz so
         * this is mostly defensive.
         */
        internal fun halfPeriodMillis(rateHz: Float): Long {
            val clamped = max(TorchWidgetConfig.MIN_RATE_HZ, min(rateHz, TorchWidgetConfig.MAX_RATE_HZ))
            return (500f / clamped).toLong().coerceAtLeast(25L)
        }
    }
}
