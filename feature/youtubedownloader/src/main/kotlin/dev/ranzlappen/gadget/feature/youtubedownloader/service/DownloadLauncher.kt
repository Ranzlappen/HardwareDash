package dev.ranzlappen.gadget.feature.youtubedownloader.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Entry point both the UI (ViewModel) and the automation
 * (`DownloadActionHandler`) use to start/cancel downloads, so neither needs to
 * know how the [DownloadForegroundService] is wired.
 */
object DownloadLauncher {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Mint a process id that doubles as the yt-dlp cancel handle. */
    fun newId(): String = "dl_${System.nanoTime()}"

    /** Queue [config] for download on the foreground service. */
    fun enqueue(context: Context, config: DownloadConfig, id: String = newId()): String {
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_ENQUEUE
            putExtra(DownloadForegroundService.EXTRA_ID, id)
            putExtra(DownloadForegroundService.EXTRA_CONFIG, json.encodeToString(config))
        }
        ContextCompat.startForegroundService(context, intent)
        return id
    }

    /** Cancel a running download by id. */
    fun cancel(context: Context, id: String) {
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_CANCEL
            putExtra(DownloadForegroundService.EXTRA_ID, id)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
