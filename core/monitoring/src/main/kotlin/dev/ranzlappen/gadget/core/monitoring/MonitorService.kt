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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Collections
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * The single, shared foreground service that samples every enabled
 * [MetricSource], persists each reading, and (per config) updates a
 * determinate notification and/or the metric's home-screen widget. There is
 * deliberately **one** such service for the whole app — features never run
 * their own monitoring service/process — so the cost of monitoring N modules
 * is one FGS, not N.
 *
 * Per metric it runs one structured coroutine on a shared dispatcher that
 * follows the metric's live config via `collectLatest` (so changing the
 * cadence/toggles restarts just that metric). A source is read in one of two
 * ways:
 *  - **Push** ([MetricSource.stream] non-null): collect the change-stream;
 *    an idle event-driven signal causes zero wakeups.
 *  - **Poll** (default): [MetricSource.sample] every `pollIntervalMs`,
 *    wrapped in [withTimeout] so one slow source can't stall the dispatcher.
 *    Charted continuous signals (the torch reference) use this so every
 *    downsample bucket has a sample to plot.
 *
 * Scaling guards: DB inserts happen on every reading (full-resolution
 * history) but **pruning is batched** ([PRUNE_INTERVAL_MS]) and **widget /
 * notification repaints are throttled** ([UI_UPDATE_THROTTLE_MS]) so a fast
 * poll rate doesn't translate into a storm of I/O or RemoteViews
 * transactions. It self-stops once no metric is enabled, so
 * [MonitorController] only ever needs to start it.
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
                        runMetric(metricKey, cfg)
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

    private suspend fun runMetric(metricKey: String, cfg: MonitorConfig) {
        val source = metricSources[metricKey] ?: return
        setActive(metricKey, on = true)
        val runtime = MetricRuntime()
        val stream = source.stream()
        if (stream != null) {
            // Push: record each change. Suits event-driven / sparse signals
            // and feeds the automation evaluator; an idle source never wakes.
            stream.collect { value ->
                record(metricKey, source.descriptor, cfg, runtime, value, System.currentTimeMillis())
            }
        } else {
            // Poll: regular cadence so every chart bucket has a sample.
            while (coroutineContext.isActive) {
                val value = sampleWithTimeout(source)
                if (value != null) {
                    record(metricKey, source.descriptor, cfg, runtime, value, System.currentTimeMillis())
                }
                delay(cfg.pollIntervalMs)
            }
        }
    }

    private suspend fun sampleWithTimeout(source: MetricSource): Float? = try {
        withTimeout(SAMPLE_TIMEOUT_MS) { source.sample() }
    } catch (_: TimeoutCancellationException) {
        null
    }

    /**
     * Persist [value] and, throttled, refresh the metric's notification +
     * widget. Inserts are never throttled (full-resolution history); pruning
     * is batched ([PRUNE_INTERVAL_MS]); UI repaints are coalesced to
     * [UI_UPDATE_THROTTLE_MS]. Called sequentially per metric (poll loop /
     * stream collector), so the timestamp guards need no synchronisation.
     */
    private suspend fun record(
        metricKey: String,
        descriptor: MetricDescriptor,
        cfg: MonitorConfig,
        runtime: MetricRuntime,
        value: Float,
        now: Long,
    ) {
        sampleRepo.insert(metricKey, now, value)
        if (now - runtime.lastPruneMs >= PRUNE_INTERVAL_MS) {
            sampleRepo.prune(metricKey, now - RETENTION_MS)
            runtime.lastPruneMs = now
        }
        val uiDue = now - runtime.lastUiMs >= UI_UPDATE_THROTTLE_MS
        if (cfg.notificationEnabled) {
            if (uiDue) postMetricNotification(metricKey, descriptor, value)
        } else {
            cancelMetricNotification(metricKey)
        }
        if (cfg.widgetEnabled && uiDue) widgetNotifiers[metricKey]?.onSample(value)
        if (uiDue) runtime.lastUiMs = now
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

    /** Per-metric run state — last prune / last UI-repaint timestamps for the
     *  batching + throttling guards. */
    private class MetricRuntime {
        var lastPruneMs: Long = 0L
        var lastUiMs: Long = 0L
    }

    private companion object {
        const val CHANNEL_ID = "monitoring"
        const val SUMMARY_NOTIFICATION_ID = 0x4D_4F_4E_31 // "MON1"
        const val METRIC_NOTIFICATION_ID_BASE = 0x4D_30_00_00 // "M0.." prefix
        const val RETENTION_MS = 24L * 60L * 60L * 1000L // 24h durable history
        const val PRUNE_INTERVAL_MS = 60L * 1000L // batch the retention sweep
        const val UI_UPDATE_THROTTLE_MS = 2_000L // coalesce widget/notification repaints
        const val SAMPLE_TIMEOUT_MS = 2_000L // a slow sample can't stall the dispatcher
    }
}
