package dev.ranzlappen.gadget.feature.torch.strobe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
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
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfigRepository
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
 * Configuration source depends on the caller:
 * - **Widget taps** pass only [EXTRA_APPWIDGET_ID]; the service reads
 *   that widget's persisted [TorchWidgetConfig] (rate / SOS) itself,
 *   off the broadcast thread. This avoids trusting a cold StateFlow
 *   cache on the provider side.
 * - **In-app controls** pass the values directly via [EXTRA_RATE_HZ]
 *   (Hz, clamped to `TorchWidgetConfig.MIN_RATE_HZ..MAX_RATE_HZ`) and
 *   [EXTRA_SOS_MODE]. When SOS is selected the loop emits a repeating
 *   Morse "SOS" ([sosTimeline]); the rate tunes the dot unit via
 *   [sosUnitMillis].
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

    @Inject
    lateinit var widgetRepository: TorchWidgetConfigRepository

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
            else -> startSession(intent)
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

    private fun startSession(intent: Intent?) {
        // Promote synchronously so the startForegroundService → startForeground
        // contract is satisfied before the suspending config read below.
        promoteToForeground()
        // Re-tap while running = no-op (the running loop already
        // honours the current rate; future "edit a live strobe"
        // UX can replace the loop without stopping by cancelling
        // the existing strobeLoop and relaunching).
        if (strobeLoop?.isActive == true) return
        isRunning = true

        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val explicitRate = intent?.getFloatExtra(EXTRA_RATE_HZ, TorchWidgetConfig.DEFAULT_RATE_HZ)
            ?: TorchWidgetConfig.DEFAULT_RATE_HZ
        val explicitSos = intent?.getBooleanExtra(EXTRA_SOS_MODE, false) ?: false

        strobeLoop = serviceScope.launch {
            // A widget tap passes only EXTRA_APPWIDGET_ID; the service reads
            // that widget's persisted config (rate / SOS) here — off the
            // broadcast thread, so a cold StateFlow cache can't strand the
            // session on stale defaults (the bug behind "SOS doesn't work").
            // In-app callers pass the rate / SOS extras directly instead.
            val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetRepository.get(appWidgetId)
            } else {
                null
            }
            val rateHz = (config?.rateHz ?: explicitRate)
                .coerceIn(TorchWidgetConfig.MIN_RATE_HZ, TorchWidgetConfig.MAX_RATE_HZ)
            val sos = config?.sosMode ?: explicitSos
            if (sos) runSosPattern(rateHz) else runConstant(rateHz)
        }
    }

    /** Constant 50%-duty strobe at [rateHz]. */
    private suspend fun runConstant(rateHz: Float) {
        val halfPeriodMs = halfPeriodMillis(rateHz)
        var on = false
        while (true) {
            on = !on
            torchController.setOn(on)
            delay(halfPeriodMs)
        }
    }

    /** Repeating Morse "SOS" (· · · — — — · · ·). The dot unit scales
     *  with [rateHz] via [sosUnitMillis] so the rate slider still tunes
     *  playback speed. Each [delay] is cancellable, so [stopStrobing]
     *  tears the loop down promptly. */
    private suspend fun runSosPattern(rateHz: Float) {
        val timeline = sosTimeline(sosUnitMillis(rateHz))
        while (true) {
            for ((on, durationMs) in timeline) {
                torchController.setOn(on)
                delay(durationMs)
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

        /** Boolean extra selecting SOS Morse playback over the constant
         *  strobe. Read in [onStartCommand]. */
        const val EXTRA_SOS_MODE = "dev.ranzlappen.gadget.feature.torch.EXTRA_SOS_MODE"

        /** Int extra carrying the tapped widget's `appWidgetId`. When
         *  present the service reads that widget's persisted config
         *  (rate / SOS) itself rather than trusting extras — robust
         *  against a cold-process StateFlow cache. */
        const val EXTRA_APPWIDGET_ID = "dev.ranzlappen.gadget.feature.torch.EXTRA_APPWIDGET_ID"

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

        /**
         * Morse "dot" unit (ms) for SOS playback, derived from [rateHz]
         * so the rate slider tunes SOS speed, then clamped to a legible
         * 60..400 ms so extreme rates stay readable as Morse.
         */
        internal fun sosUnitMillis(rateHz: Float): Long {
            val clamped = max(TorchWidgetConfig.MIN_RATE_HZ, min(rateHz, TorchWidgetConfig.MAX_RATE_HZ))
            return (1000f / clamped).toLong().coerceIn(60L, 400L)
        }

        /**
         * Build one full SOS cycle as an ordered list of
         * `(torchOn, durationMs)` steps using standard Morse timing
         * relative to [unit]: dot = 1u on, dash = 3u on, gap between
         * elements = 1u off, gap between letters = 3u off, and a 7u off
         * tail before the pattern repeats.
         */
        internal fun sosTimeline(unit: Long): List<Pair<Boolean, Long>> {
            val dot = unit
            val dash = unit * 3
            val elementGap = unit
            val letterGap = unit * 3
            val wordGap = unit * 7

            fun letter(elements: List<Long>): List<Pair<Boolean, Long>> = buildList {
                elements.forEachIndexed { index, onMs ->
                    add(true to onMs)
                    if (index != elements.lastIndex) add(false to elementGap)
                }
            }

            val s = letter(listOf(dot, dot, dot))
            val o = letter(listOf(dash, dash, dash))
            return buildList {
                addAll(s); add(false to letterGap)
                addAll(o); add(false to letterGap)
                addAll(s); add(false to wordGap)
            }
        }
    }
}
