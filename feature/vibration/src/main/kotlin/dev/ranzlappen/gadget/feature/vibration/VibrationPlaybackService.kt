package dev.ranzlappen.gadget.feature.vibration

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that plays a widget's configured vibration — a one-shot
 * buzz (the [VibrationWidgetConfig.FUNCTION_ONESHOT] function) or a saved
 * [VibrationPattern] (the [VibrationWidgetConfig.FUNCTION_PATTERN] function).
 *
 * **This is the foreground context every widget-tap vibration runs in.** A
 * widget tap is a background broadcast, and a plain `Vibrator.vibrate()` from
 * the background is silently dropped by the OS (Android 12+). So
 * [VibrationActionHandler] starts this service with the already-resolved
 * [EXTRA_ACTION_KEY] + params and the buzz plays from here (mirroring how
 * torch's strobe runs through its own FGS). The legacy [EXTRA_APPWIDGET_ID]
 * path — the service reading a widget's persisted [VibrationWidgetConfig] via
 * `getFresh` — is retained for any caller that still starts it that way.
 * All playback flows through [VibrationController], which folds the commanded
 * amplitude into [VibrationRuntime] (the monitored signal).
 *
 * `foregroundServiceType="shortService"` (API 34+) permits a user-initiated FGS
 * with a ~3-minute cap — an acceptable safety bound for a vibration session.
 */
@AndroidEntryPoint
class VibrationPlaybackService : Service() {

    @Inject
    lateinit var controller: VibrationController

    @Inject
    lateinit var widgetRepository: WidgetConfigStore<VibrationWidgetConfig>

    @Inject
    lateinit var patternRepository: PatternRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var playbackJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startSession(intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        controller.stop()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startSession(intent: Intent?) {
        promoteToForeground()
        if (playbackJob?.isActive == true) return

        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        playbackJob = serviceScope.launch {
            val directAction = intent?.getStringExtra(EXTRA_ACTION_KEY)
            if (directAction != null) {
                // Started by VibrationActionHandler (widget tap / automation):
                // the action + params are already resolved, so play them from
                // this foreground-service context — a plain Vibrator.vibrate()
                // on the background broadcast path is silently dropped by the
                // OS (Android 12+ background-vibration policy).
                playDirect(directAction, intent)
                stopSelf()
                return@launch
            }
            val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetRepository.getFresh(appWidgetId)
            } else {
                null
            }
            // Legacy path: a non-widget caller that passes only an appWidgetId.
            // It branches on the function-driven `actionKey` + `params` (the v2
            // config shape).
            when (config?.actionKey) {
                VibrationWidgetConfig.FUNCTION_PATTERN ->
                    playPatternById(config.params[VibrationActionHandler.PARAM_PATTERN_ID])
                else -> {
                    // Default + one-shot function: a buzz at the configured
                    // strength/duration (falls back to defaults for an in-app
                    // start with no config).
                    val amplitude = config?.params
                        ?.get(VibrationActionHandler.PARAM_AMPLITUDE)?.toIntOrNull()
                        ?: VibrationWidgetConfig.DEFAULT_AMPLITUDE_PERCENT
                    val duration = config?.params
                        ?.get(VibrationActionHandler.PARAM_DURATION_MS)?.toLongOrNull()
                        ?: VibrationWidgetConfig.DEFAULT_DURATION_MS
                    controller.oneShot(amplitudePercent = amplitude, durationMillis = duration)
                }
            }
            // One-shots / non-looping patterns are fire-and-forget — the
            // VibrationRuntime decay models the tail; tear the FGS down.
            stopSelf()
        }
    }

    /** Play the already-resolved [actionKey] + extras from this foreground
     *  context (the VibrationActionHandler dispatch path). */
    private suspend fun playDirect(actionKey: String, intent: Intent) {
        when (actionKey) {
            VibrationActionHandler.ACTION_PATTERN_PLAY ->
                playPatternById(intent.getStringExtra(EXTRA_PATTERN_ID))
            else -> controller.oneShot(
                amplitudePercent = intent.getIntExtra(
                    EXTRA_AMPLITUDE,
                    VibrationWidgetConfig.DEFAULT_AMPLITUDE_PERCENT,
                ),
                durationMillis = intent.getLongExtra(
                    EXTRA_DURATION_MS,
                    VibrationWidgetConfig.DEFAULT_DURATION_MS,
                ),
            )
        }
    }

    private suspend fun playPatternById(patternId: String?) {
        val pattern = patternId?.takeIf { it.isNotBlank() }?.let { patternRepository.get(it) } ?: return
        controller.playPattern(
            timingsMillis = pattern.timingsMillis.toLongArray(),
            amplitudes = pattern.amplitudes.toIntArray(),
            loop = false,
        )
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        controller.stop()
    }

    private fun promoteToForeground() {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.vibration_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.vibration_notification_title))
            .setContentText(getString(R.string.vibration_notification_text))
            .setSmallIcon(R.drawable.ic_vibration_on)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_vibration_off,
                getString(R.string.vibration_notification_stop),
                buildStopIntent(),
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()

    private fun buildStopIntent(): PendingIntent {
        val intent = Intent(this, VibrationPlaybackService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(
            this,
            /* requestCode = */ 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "vibration_playback"
        const val NOTIFICATION_ID = 0x56_42_5F_53 // "VB_S"
        const val ACTION_STOP = "dev.ranzlappen.gadget.feature.vibration.PLAYBACK_STOP"

        /** Int extra carrying the tapped widget's `appWidgetId`. When present
         *  the service reads that widget's persisted config itself. */
        const val EXTRA_APPWIDGET_ID = "dev.ranzlappen.gadget.feature.vibration.EXTRA_APPWIDGET_ID"

        /** String extra naming the already-resolved action to play
         *  ([VibrationActionHandler.ACTION_ONESHOT] /
         *  [VibrationActionHandler.ACTION_PATTERN_PLAY]) — the dispatch path. */
        const val EXTRA_ACTION_KEY = "dev.ranzlappen.gadget.feature.vibration.EXTRA_ACTION_KEY"

        /** One-shot amplitude percent (Int) for an [EXTRA_ACTION_KEY] start. */
        const val EXTRA_AMPLITUDE = "dev.ranzlappen.gadget.feature.vibration.EXTRA_AMPLITUDE"

        /** One-shot duration ms (Long) for an [EXTRA_ACTION_KEY] start. */
        const val EXTRA_DURATION_MS = "dev.ranzlappen.gadget.feature.vibration.EXTRA_DURATION_MS"

        /** Saved-pattern id (String) for an [EXTRA_ACTION_KEY] pattern start. */
        const val EXTRA_PATTERN_ID = "dev.ranzlappen.gadget.feature.vibration.EXTRA_PATTERN_ID"
    }
}
