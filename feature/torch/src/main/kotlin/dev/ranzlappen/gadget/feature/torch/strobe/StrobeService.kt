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
 *   that widget's persisted [TorchWidgetConfig] (rate + Morse mode /
 *   message) itself, straight from DataStore. A widget in Morse mode
 *   loops its message through [MorseCodec] (defaulting to
 *   [DEFAULT_MORSE_TEXT] = "SOS" when the box is blank); otherwise it
 *   runs a constant strobe.
 * - **In-app controls** pass the values directly via [EXTRA_RATE_HZ]
 *   (Hz, clamped to `TorchWidgetConfig.MIN_RATE_HZ..MAX_RATE_HZ`) and an
 *   optional [EXTRA_MORSE_TEXT]; when present the loop plays that text
 *   as Morse. The rate tunes the dot unit via [morseUnitMillis].
 *
 * Lifecycle:
 * - Live strobing state is published to the injected [StrobeRuntime]
 *   `StateFlow` (`setRunning(true)` once the loop launches, `false`
 *   in [stopStrobing] / [onDestroy] / [onTimeout]). The screen folds
 *   it into its state and `StrobeWidgetProvider` reads it to decide
 *   whether a widget tap starts a new run or stops the existing one —
 *   no polling, and no stale-after-kill (the runtime dies with the
 *   process, so a cold read is always correct).
 *
 * Foreground-service contract:
 * - `foregroundServiceType="shortService"` in the manifest so the OS
 *   permits flash access while in foreground state on API 34+ without
 *   a camera-typed FGS; the ~3-minute cap is an acceptable safety
 *   bound for a strobe session.
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

    @Inject
    lateinit var strobeRuntime: StrobeRuntime

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
        strobeRuntime.setRunning(true)

        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val explicitRate = intent?.getFloatExtra(EXTRA_RATE_HZ, TorchWidgetConfig.DEFAULT_RATE_HZ)
            ?: TorchWidgetConfig.DEFAULT_RATE_HZ
        val explicitMorse = intent?.getStringExtra(EXTRA_MORSE_TEXT)

        strobeLoop = serviceScope.launch {
            // A widget tap passes only EXTRA_APPWIDGET_ID; read that
            // widget's persisted config straight from DataStore
            // (getFresh, not the hot cache) so a just-pinned widget plays
            // its Morse message on the very first tap. In-app callers
            // pass the values directly via the extras.
            val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetRepository.getFresh(appWidgetId)
            } else {
                null
            }
            val rateHz = (config?.rateHz ?: explicitRate)
                .coerceIn(TorchWidgetConfig.MIN_RATE_HZ, TorchWidgetConfig.MAX_RATE_HZ)
            // Morse resolution:
            //  - in-app Morse button passes EXTRA_MORSE_TEXT directly;
            //  - a widget in Morse mode plays its message, defaulting to
            //    "SOS" when the box was left blank;
            //  - otherwise it's a plain constant strobe.
            val morse = when {
                !explicitMorse.isNullOrBlank() -> explicitMorse
                config != null && config.morseMode -> config.morseText.ifBlank { DEFAULT_MORSE_TEXT }
                else -> null
            }
            if (morse != null) runMorse(morse, rateHz) else runConstant(rateHz)
        }
    }

    /** Repeating Morse playback of arbitrary [text]; falls back to a
     *  constant strobe if nothing in the text is encodable. */
    private suspend fun runMorse(text: String, rateHz: Float) {
        val timeline = MorseCodec.toTimeline(text, morseUnitMillis(rateHz))
        if (timeline.isEmpty()) {
            runConstant(rateHz)
            return
        }
        while (true) {
            for ((on, durationMs) in timeline) {
                torchController.setOn(on)
                delay(durationMs)
            }
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

    private fun stopStrobing() {
        strobeLoop?.cancel()
        strobeLoop = null
        // Final off — synchronous, no coroutine needed.
        torchController.setOn(false)
        strobeRuntime.setRunning(false)
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
     * [StrobeRuntime] signal.
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
            // A foreground service MUST post a notification on API 26+
            // (OS requirement — it can't be suppressed in the standard
            // flavor). DEFERRED lets the OS hold it back ~10s, so a brief
            // strobe the user toggles straight off never surfaces one.
            // The rooted flavor avoids the notification entirely by
            // running the strobe outside an FGS.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
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

        /** Int extra carrying the tapped widget's `appWidgetId`. When
         *  present the service reads that widget's persisted config
         *  (rate / Morse mode + message) itself rather than trusting
         *  extras — robust against a cold-process cache. */
        const val EXTRA_APPWIDGET_ID = "dev.ranzlappen.gadget.feature.torch.EXTRA_APPWIDGET_ID"

        /** String extra: arbitrary text to play as Morse, looped. Takes
         *  precedence over the constant strobe when non-blank. */
        const val EXTRA_MORSE_TEXT = "dev.ranzlappen.gadget.feature.torch.EXTRA_MORSE_TEXT"

        /** Fallback Morse message for a widget left in Morse mode with
         *  an empty message box. */
        const val DEFAULT_MORSE_TEXT = "SOS"

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
         * Morse "dot" unit (ms) derived from [rateHz] so the rate slider
         * tunes Morse playback speed, then clamped to a legible
         * 60..400 ms so extreme rates stay readable as Morse.
         */
        internal fun morseUnitMillis(rateHz: Float): Long {
            val clamped = max(TorchWidgetConfig.MIN_RATE_HZ, min(rateHz, TorchWidgetConfig.MAX_RATE_HZ))
            return (1000f / clamped).toLong().coerceIn(60L, 400L)
        }
    }
}
