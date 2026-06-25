package dev.ranzlappen.gadget.feature.youtubedownloader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.youtubedownloader.cookies.CookieStore
import dev.ranzlappen.gadget.feature.youtubedownloader.service.DownloadLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable view-state for the downloader screen. */
data class YoutubeDownloaderUiState(
    val config: DownloadConfig = DownloadConfig(),
    val tasks: List<DownloadTask> = emptyList(),
    val hasCookies: Boolean = false,
)

/** All user intents the stateless screen can raise. */
sealed interface YtdlEvent {
    data class UrlChanged(val url: String) : YtdlEvent
    data class KindChanged(val kind: MediaKind) : YtdlEvent
    data class VideoQualityChanged(val quality: VideoQuality) : YtdlEvent
    data class AudioFormatChanged(val format: AudioFormat) : YtdlEvent
    data class AudioQualityChanged(val quality: AudioQuality) : YtdlEvent
    data class ScopeChanged(val scope: PlaylistScope) : YtdlEvent
    data class PlaylistItemsChanged(val items: String) : YtdlEvent
    data object ToggleThumbnail : YtdlEvent
    data object ToggleMetadata : YtdlEvent
    data object ToggleChapters : YtdlEvent
    data object ToggleSponsorblock : YtdlEvent
    data object ToggleRestrict : YtdlEvent
    data object ToggleUseCookies : YtdlEvent
    data object StartDownload : YtdlEvent
    data class Cancel(val id: String) : YtdlEvent
    data object ClearFinished : YtdlEvent
    data object SignOut : YtdlEvent
}

@HiltViewModel
class YoutubeDownloaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: YoutubeDlEngine,
    private val cookieStore: CookieStore,
    private val settings: DownloaderSettingsRepository,
) : ViewModel() {

    private val configFlow = MutableStateFlow(DownloadConfig())

    val state: StateFlow<YoutubeDownloaderUiState> = combine(
        configFlow,
        engine.tasks,
        cookieStore.present,
    ) { config, tasks, hasCookies ->
        YoutubeDownloaderUiState(
            config = config,
            // Latest first; map preserves insertion order.
            tasks = tasks.values.toList().asReversed(),
            hasCookies = hasCookies,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), YoutubeDownloaderUiState())

    init {
        // Restore the last-used form.
        viewModelScope.launch { configFlow.value = settings.lastConfig.first() }
        // When the user signs in, default to using those cookies. Only fires on
        // transitions, so a later manual toggle-off is respected.
        viewModelScope.launch {
            cookieStore.present.collect { present ->
                if (present && !configFlow.value.useCookies) update { it.copy(useCookies = true) }
            }
        }
    }

    fun onEvent(event: YtdlEvent) {
        when (event) {
            is YtdlEvent.UrlChanged -> update { it.copy(url = event.url) }
            is YtdlEvent.KindChanged -> update { it.copy(kind = event.kind) }
            is YtdlEvent.VideoQualityChanged -> update { it.copy(videoQuality = event.quality) }
            is YtdlEvent.AudioFormatChanged -> update { it.copy(audioFormat = event.format) }
            is YtdlEvent.AudioQualityChanged -> update { it.copy(audioQuality = event.quality) }
            is YtdlEvent.ScopeChanged -> update { it.copy(scope = event.scope) }
            is YtdlEvent.PlaylistItemsChanged -> update { it.copy(playlistItems = event.items) }
            YtdlEvent.ToggleThumbnail -> update { it.copy(embedThumbnail = !it.embedThumbnail) }
            YtdlEvent.ToggleMetadata -> update { it.copy(embedMetadata = !it.embedMetadata) }
            YtdlEvent.ToggleChapters -> update { it.copy(embedChapters = !it.embedChapters) }
            YtdlEvent.ToggleSponsorblock -> update { it.copy(sponsorblockRemove = !it.sponsorblockRemove) }
            YtdlEvent.ToggleRestrict -> update { it.copy(restrictFilenames = !it.restrictFilenames) }
            YtdlEvent.ToggleUseCookies -> update { it.copy(useCookies = !it.useCookies) }
            YtdlEvent.StartDownload -> startDownload()
            is YtdlEvent.Cancel -> DownloadLauncher.cancel(context, event.id)
            YtdlEvent.ClearFinished -> engine.clearFinished()
            YtdlEvent.SignOut -> viewModelScope.launch { cookieStore.clear() }
        }
    }

    /** Persist cookies captured by the in-app login WebView. */
    fun onCookiesCaptured(netscape: String) {
        viewModelScope.launch {
            cookieStore.write(netscape)
            update { it.copy(useCookies = true) }
        }
    }

    private fun startDownload() {
        val config = configFlow.value
        if (config.url.isBlank()) return
        viewModelScope.launch { settings.save(config) }
        DownloadLauncher.enqueue(context, config)
    }

    private fun update(transform: (DownloadConfig) -> DownloadConfig) {
        configFlow.update(transform)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
