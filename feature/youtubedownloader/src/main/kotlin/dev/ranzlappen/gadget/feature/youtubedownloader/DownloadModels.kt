package dev.ranzlappen.gadget.feature.youtubedownloader

import kotlinx.serialization.Serializable

/**
 * Domain model for the YouTube downloader.
 *
 * [DownloadConfig] is the serializable request description — it travels into
 * the foreground service as a JSON intent extra and is persisted as the
 * user's last-used form. [DownloadTask] is the in-memory runtime view of a
 * download as it progresses; it is never persisted.
 *
 * The option set mirrors `tools.ranzlappen.com`'s YouTube MP3 Studio
 * (`youtube-mp3/tool.js` `buildTokens()`), ported to typed Kotlin.
 */

/** Whether to keep the full video or extract audio only. */
enum class MediaKind { VIDEO, AUDIO }

/**
 * Video quality ceiling, expressed as a yt-dlp `-f` format selector. We always
 * ask for the best video+audio under the height cap and fall back to a
 * progressive stream, then remux to MP4 via ffmpeg.
 */
enum class VideoQuality(val formatSelector: String) {
    BEST("bv*+ba/b"),
    P1080("bv*[height<=1080]+ba/b[height<=1080]/b"),
    P720("bv*[height<=720]+ba/b[height<=720]/b"),
    P480("bv*[height<=480]+ba/b[height<=480]/b"),
}

/** Audio container/codec passed to `--audio-format`. */
enum class AudioFormat(val ytdlp: String) {
    MP3("mp3"),
    M4A("m4a"),
    OPUS("opus"),
    BEST("best"),
}

/** Audio bitrate passed to `--audio-quality` (only meaningful for MP3). */
enum class AudioQuality(val value: String) {
    Q320("320K"),
    Q256("256K"),
    Q192("192K"),
    Q128("128K"),
}

/** Single video vs. whole playlist. */
enum class PlaylistScope { SINGLE, PLAYLIST }

/**
 * A fully-specified download request. Defaults reproduce the YouTube MP3
 * Studio "playlist" preset but for video.
 */
@Serializable
data class DownloadConfig(
    val url: String = "",
    val kind: MediaKind = MediaKind.VIDEO,
    val videoQuality: VideoQuality = VideoQuality.P1080,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val audioQuality: AudioQuality = AudioQuality.Q320,
    val scope: PlaylistScope = PlaylistScope.SINGLE,
    /** yt-dlp `--playlist-items` range, e.g. "1-10,15". Blank = all. */
    val playlistItems: String = "",
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedChapters: Boolean = false,
    val sponsorblockRemove: Boolean = false,
    val restrictFilenames: Boolean = false,
    /** Attach the captured cookies.txt so private/members-only items resolve. */
    val useCookies: Boolean = false,
)

/** Lifecycle of a single download. */
enum class DownloadStatus { Queued, Running, Completed, Failed, Cancelled }

/**
 * Runtime view of an in-flight (or finished) download. [id] doubles as the
 * yt-dlp process id used to cancel via `destroyProcessById`.
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val config: DownloadConfig,
    val status: DownloadStatus = DownloadStatus.Queued,
    /** 0..100. yt-dlp reports per-file progress for playlists. */
    val progress: Float = 0f,
    val etaSeconds: Long = -1L,
    /** The most recent raw yt-dlp status line, for display/debug. */
    val lastLine: String = "",
    val error: String? = null,
)
