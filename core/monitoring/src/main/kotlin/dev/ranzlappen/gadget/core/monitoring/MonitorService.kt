package dev.ranzlappen.gadget.core.monitoring

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.data.MonitorSampleRepository
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * Foreground service that samples every enabled [MetricSource] on its
 * configured cadence, persists each reading, and (per config) updates a
 * determinate notification and/or the metric's home-screen widget.
 *
 * It self-stops once no metric is enabled, so [MonitorController] only
 * ever needs to start it. The sampling loop honours each metric's live
 * config via `collectLatest`, so changing the poll interval / toggles
 * restarts that metric's loop without bouncing the whole service.
 */
@AndroidEntryPoint
class MonitorService : Service() {

    @Inject lateinit var configRepo: MonitorConfigRepository
    @Inject lateinit var sampleRepo: MonitorSampleRepository
    @Inject lateinit var metricSources: Map<String, @JvmSuppressWildcards MetricSource>
    @Inject lateinit var widgetNotifiers: Map<String, @JvmSuppressWildcards MonitorWidgetNotifier>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = Collections.synchronizedSet(mutableSetOf<String>())
    private val notifiedIds = Collections.synchronizedSet(mutableSetOf<Int>())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(SUMMARY_NOTIFICATION_ID, summaryNotification())
        metricSources.keys.forEach { metricKey ->
            scope.launch {
                configRepo.config(metricKey).collectLatest { cfg ->
                    if (cfg.enabled) {
                        sampleLoop(metricKey, cfg)
                    } else {
                        cancelMetricNotification(metricKey)
                        setActive(metricKey, on = false)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        val mgr = NotificationManagerCompat.from(this)
        synchronized(notifiedIds) { notifiedIds.toList() }.forEach { mgr.cancel(it) }
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun sampleLoop(metricKey: String, cfg: MonitorConfig) {
        val source = metricSources[metricKey] ?: return
        setActive(metricKey, on = true)
        while (coroutineContext.isActive) {
            val value = source.sample()
            val now = System.currentTimeMillis()
            sampleRepo.insert(metricKey, now, value)
            sampleRepo.prune(metricKey, now - RETENTION_MS)
            if (cfg.notificationEnabled) postMetricNotification(metricKey, source.descriptor, value)
            else cancelMetricNotification(metricKey)
            if (cfg.widgetEnabled) widgetNotifiers[metricKey]?.onSample(value)
            delay(cfg.pollIntervalMs)
        }
    }

    private fun setActive(metricKey: String, on: Boolean) {
        if (on) active.add(metricKey) else active.remove(metricKey)
        if (active.isEmpty()) {
            stopSelf()
        } else if (canPostNotifications()) {
            NotificationManagerCompat.from(this).notify(SUMMARY_NOTIFICATION_ID, summaryNotification())
        }
    }

    // -- notifications ----------------------------------------------------

    private fun summaryNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.monitor_notification_summary_title))
            .setContentText(
                resources.getQuantityString(
                    R.plurals.monitor_notification_summary_text,
                    active.size.coerceAtLeast(1),
                    active.size.coerceAtLeast(1),
                ),
            )
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun postMetricNotification(metricKey: String, descriptor: MetricDescriptor, value: Float) {
        if (!canPostNotifications()) return
        val span = (descriptor.max - descriptor.min).takeIf { it > 0f } ?: 1f
        val progress = (((value - descriptor.min) / span) * 100f).roundToInt().coerceIn(0, 100)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(descriptor.displayName)
            .setContentText(formatValue(value, descriptor))
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        val id = metricNotificationId(metricKey)
        notifiedIds.add(id)
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun cancelMetricNotification(metricKey: String) {
        val id = metricNotificationId(metricKey)
        if (notifiedIds.remove(id)) NotificationManagerCompat.from(this).cancel(id)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.monitor_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun formatValue(value: Float, descriptor: MetricDescriptor): String = buildString {
        append(value.roundToInt())
        if (descriptor.unit.isNotEmpty()) append(descriptor.unit)
    }

    private fun metricNotificationId(metricKey: String): Int =
        METRIC_NOTIFICATION_ID_BASE + (metricKey.hashCode() and 0xFFFF)

    private companion object {
        const val CHANNEL_ID = "monitoring"
        const val SUMMARY_NOTIFICATION_ID = 0x4D_4F_4E_31 // "MON1"
        const val METRIC_NOTIFICATION_ID_BASE = 0x4D_30_00_00 // "M0.." prefix
        const val RETENTION_MS = 24L * 60L * 60L * 1000L // 24h durable history
    }
}
