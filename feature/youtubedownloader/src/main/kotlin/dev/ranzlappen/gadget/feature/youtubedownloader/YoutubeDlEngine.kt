package dev.ranzlappen.gadget.feature.youtubedownloader

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin coroutine wrapper around the bundled yt-dlp + ffmpeg runtime.
 *
 * Owns the one-time native init (unpacking the Python runtime), a single
 * source-of-truth [tasks] map keyed by process id, and the blocking
 * `execute`/`destroyProcessById` calls hoisted onto [Dispatchers.IO].
 *
 * `@Singleton` so the foreground service, the ViewModel, the
 * [DownloadMetricSource] and the [dev.ranzlappen.gadget.feature.youtubedownloader.automation.DownloadActionHandler]
 * all observe the same task state.
 */
@Singleton
class YoutubeDlEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val initMutex = Mutex()

    @Volatile
    private var initialized = false

    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())

    /** Live view of every download this process has started, keyed by id. */
    val tasks: StateFlow<Map<String, DownloadTask>> = _tasks.asStateFlow()

    /**
     * Idempotently unpack and initialise yt-dlp + ffmpeg. Safe to call before
     * every download; the [initMutex] + [initialized] guard make repeat calls
     * cheap. Runs on IO because the first call extracts the Python runtime.
     */
    suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
            }
            initialized = true
        }
    }

    /**
     * Run [request] to completion, mirroring progress into [tasks] under
     * [task].id. Suspends until the download finishes, fails, or is cancelled.
     * Never throws — terminal state is reflected in the task's [DownloadStatus].
     */
    suspend fun download(task: DownloadTask, request: YoutubeDLRequest) {
        ensureInitialized()
        put(task.copy(status = DownloadStatus.Running, progress = 0f))
        try {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().execute(request, task.id) { progress, eta, line ->
                    patch(task.id) {
                        it.copy(
                            status = DownloadStatus.Running,
                            progress = progress.coerceIn(0f, 100f),
                            etaSeconds = eta,
                            lastLine = line,
                        )
                    }
                }
            }
            patch(task.id) { it.copy(status = DownloadStatus.Completed, progress = 100f) }
        } catch (e: YoutubeDL.CanceledException) {
            Timber.i(e, "yt-dlp download cancelled: %s", task.id)
            patch(task.id) { it.copy(status = DownloadStatus.Cancelled) }
        } catch (e: YoutubeDLException) {
            Timber.w(e, "yt-dlp download failed: %s", task.id)
            patch(task.id) { it.copy(status = DownloadStatus.Failed, error = e.message) }
        } catch (e: InterruptedException) {
            Timber.w(e, "yt-dlp download interrupted: %s", task.id)
            patch(task.id) { it.copy(status = DownloadStatus.Cancelled) }
        }
    }

    /** Request cancellation of a running download by id. */
    fun cancel(id: String): Boolean = YoutubeDL.getInstance().destroyProcessById(id)

    /** Cancel every currently-running download. */
    fun cancelAll() {
        _tasks.value.values
            .filter { it.status == DownloadStatus.Running }
            .forEach { cancel(it.id) }
    }

    /** Drop finished/failed/cancelled tasks from the visible list. */
    fun clearFinished() {
        _tasks.update { current ->
            current.filterValues { it.status == DownloadStatus.Running || it.status == DownloadStatus.Queued }
        }
    }

    private fun put(task: DownloadTask) = _tasks.update { it + (task.id to task) }

    private fun patch(id: String, transform: (DownloadTask) -> DownloadTask) =
        _tasks.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to transform(existing))
        }
}
