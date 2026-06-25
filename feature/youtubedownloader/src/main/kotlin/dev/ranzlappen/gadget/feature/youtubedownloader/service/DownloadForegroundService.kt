package dev.ranzlappen.gadget.feature.youtubedownloader.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.notifications.ChannelSpec
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadConfig
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadStatus
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadTask
import dev.ranzlappen.gadget.feature.youtubedownloader.R
import dev.ranzlappen.gadget.feature.youtubedownloader.YoutubeDlEngine
import dev.ranzlappen.gadget.feature.youtubedownloader.YtDlpRequestBuilder
import dev.ranzlappen.gadget.feature.youtubedownloader.cookies.CookieStore
import dev.ranzlappen.gadget.feature.youtubedownloader.storage.MediaStoreExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * `dataSync` foreground service that runs yt-dlp downloads so they survive the
 * app being backgrounded. One persistent determinate-progress notification
 * reflects the active download; the service self-stops when the last download
 * finishes.
 *
 * The actual work is delegated to the `@Singleton` [YoutubeDlEngine] so the
 * UI and automation observe the same task state — the service only owns the
 * Android lifecycle (foreground promotion + notification).
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var engine: YoutubeDlEngine
    @Inject lateinit var cookieStore: CookieStore
    @Inject lateinit var channels: NotificationChannelRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val active = AtomicInteger(0)
    private var foregrounded = false
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> handleEnqueue(intent)
            ACTION_CANCEL -> intent.getStringExtra(EXTRA_ID)?.let(engine::cancel)
            else -> stopIfIdle()
        }
        return START_NOT_STICKY
    }

    private fun handleEnqueue(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID)
        val configJson = intent.getStringExtra(EXTRA_CONFIG)
        val config = configJson?.let { runCatching { json.decodeFromString<DownloadConfig>(it) }.getOrNull() }
        if (id == null || config == null || config.url.isBlank()) {
            stopIfIdle()
            return
        }

        promoteToForeground(initialNotification(config))
        active.incrementAndGet()
        serviceScope.launch {
            // Download into a private working dir, then publish finished files
            // into the shared MediaStore collections.
            val workDir = File(filesDir, "$WORK_SUBDIR/$id").apply { mkdirs() }
            val cookies = if (config.useCookies) cookieStore.fileOrNull()?.absolutePath else null
            val request = YtDlpRequestBuilder.toRequest(config, config.url, workDir.absolutePath, cookies)
            val task = DownloadTask(id = id, url = config.url, title = config.url, config = config)

            val notifyJob = launch {
                engine.tasks
                    .map { it[id] }
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect { updateNotification(it) }
            }
            engine.download(task, request)
            notifyJob.cancel()

            if (engine.tasks.value[id]?.status == DownloadStatus.Completed) {
                runCatching { MediaStoreExporter.publish(applicationContext, workDir, config.kind) }
                    .onSuccess { Timber.i("Exported %d file(s) to MediaStore", it) }
                    .onFailure { Timber.w(it, "MediaStore export failed") }
            }
            workDir.deleteRecursively()

            if (active.decrementAndGet() == 0) {
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun stopIfIdle() {
        if (active.get() == 0) {
            stopForegroundCompat()
            stopSelf()
        }
    }

    // ─── Notification ───────────────────────────────────────────────────

    private fun promoteToForeground(notification: android.app.Notification) {
        channels.ensure(channelSpec())
        if (foregrounded) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregrounded = true
    }

    /** Re-issuing the FGS notification updates progress without POST_NOTIFICATIONS. */
    private fun updateNotification(task: DownloadTask) {
        val notification = buildNotification(task)
        if (!foregrounded) {
            promoteToForeground(notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun channelSpec() = ChannelSpec(
        id = CHANNEL_ID,
        displayName = getString(R.string.ytdl_notification_channel_name),
        description = getString(R.string.ytdl_notification_channel_desc),
        importance = ChannelSpec.Importance.Low,
    )

    private fun initialNotification(config: DownloadConfig): android.app.Notification =
        buildNotification(
            DownloadTask(id = "", url = config.url, title = config.url, config = config, status = DownloadStatus.Queued),
        )

    private fun buildNotification(task: DownloadTask): android.app.Notification {
        val statusText = when (task.status) {
            DownloadStatus.Completed -> getString(R.string.ytdl_notification_done)
            DownloadStatus.Failed -> getString(R.string.ytdl_notification_failed)
            DownloadStatus.Cancelled -> getString(R.string.ytdl_notification_cancelled)
            else -> getString(R.string.ytdl_notification_downloading, task.progress.toInt())
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ytdl_notification_title))
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(task.status == DownloadStatus.Running || task.status == DownloadStatus.Queued)
            .setProgress(100, task.progress.toInt(), task.status == DownloadStatus.Queued)

        if (task.id.isNotEmpty() &&
            (task.status == DownloadStatus.Running || task.status == DownloadStatus.Queued)
        ) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.ytdl_notification_cancel),
                cancelIntent(task.id),
            )
        }
        return builder.build()
    }

    private fun cancelIntent(id: String): PendingIntent {
        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getService(
            this,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (!foregrounded) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        foregrounded = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_ENQUEUE = "dev.ranzlappen.gadget.feature.youtubedownloader.ENQUEUE"
        const val ACTION_CANCEL = "dev.ranzlappen.gadget.feature.youtubedownloader.CANCEL"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_CONFIG = "extra_config"

        private const val WORK_SUBDIR = "ytdl_work"
        private const val CHANNEL_ID = "youtube_downloads"
        private const val NOTIFICATION_ID = 0x59_54_44_4C // "YTDL"
    }
}
