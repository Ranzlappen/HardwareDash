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
 * buzz ([VibrationWidgetConfig] for [dev.ranzlappen.gadget.feature.vibration.widget.WidgetType.Vibrate])
 * or a saved [VibrationPattern] ([WidgetType.Pattern]).
 *
 * Mirror of torch's `StrobeService`:
 * - **Widget taps** pass only [EXTRA_APPWIDGET_ID]; the service reads that
 *   widget's persisted [VibrationWidgetConfig] itself (via `getFresh`, not the
 *   hot cache, so a just-pinned widget plays correctly on the first tap).
 * - All playback flows through [VibrationController], which folds the commanded
 *   amplitude into [VibrationRuntime] (the monitored signal).
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
            val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetRepository.getFresh(appWidgetId)
            } else {
                null
            }
            when (config?.type) {
                dev.ranzlappen.gadget.feature.vibration.widget.WidgetType.Pattern -> {
                    val pattern = config.patternId.takeIf { it.isNotBlank() }
                        ?.let { patternRepository.get(it) }
                    if (pattern != null) {
                        controller.playPattern(
                            timingsMillis = pattern.timingsMillis.toLongArray(),
                            amplitudes = pattern.amplitudes.toIntArray(),
                            loop = false,
                        )
                    }
                }
                else -> {
                    // Default + Vibrate variant: a one-shot at the configured
                    // strength/duration (falls back to defaults for an in-app
                    // start with no config).
                    controller.oneShot(
                        amplitudePercent = config?.amplitudePercent
                            ?: VibrationWidgetConfig.DEFAULT_AMPLITUDE_PERCENT,
                        durationMillis = config?.durationMillis
                            ?: VibrationWidgetConfig.DEFAULT_DURATION_MS,
                    )
                }
            }
            // One-shots / non-looping patterns are fire-and-forget — the
            // VibrationRuntime decay models the tail; tear the FGS down.
            stopSelf()
        }
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
    }
}
