package dev.ranzlappen.gadget.feature.youtubedownloader

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * Turns a [DownloadConfig] into yt-dlp arguments — a typed Kotlin port of
 * `tools.ranzlappen.com`'s `youtube-mp3/tool.js` `buildTokens()`.
 *
 * [buildArgs] is the pure, unit-testable core (no Android, no yt-dlp types);
 * [toRequest] is the thin adapter that feeds those args to a
 * [YoutubeDLRequest]. Keeping the two split lets the option logic be asserted
 * in plain JVM tests without the native library on the classpath.
 */
object YtDlpRequestBuilder {

    /** Output template for a single item: "<title>.<ext>". */
    const val TEMPLATE_SINGLE: String = "%(title)s.%(ext)s"

    /** Output template for a playlist: "<playlist>/01 - <title>.<ext>". */
    const val TEMPLATE_PLAYLIST: String =
        "%(playlist_title)s/%(playlist_index)02d - %(title)s.%(ext)s"

    /**
     * Build the full yt-dlp argument list (excluding the URL, which yt-dlp
     * takes positionally via the request constructor).
     *
     * @param outputDir absolute directory downloads are written into.
     * @param cookiesPath absolute path to a Netscape cookies.txt, or null.
     */
    fun buildArgs(
        config: DownloadConfig,
        outputDir: String,
        cookiesPath: String?,
    ): List<String> = buildList {
        when (config.kind) {
            MediaKind.AUDIO -> {
                add("-x")
                add("--audio-format"); add(config.audioFormat.ytdlp)
                if (config.audioFormat == AudioFormat.MP3) {
                    add("--audio-quality"); add(config.audioQuality.value)
                }
                add("-f"); add("bestaudio/best")
            }
            MediaKind.VIDEO -> {
                add("-f"); add(config.videoQuality.formatSelector)
                // Remux separate video+audio streams into a single MP4.
                add("--merge-output-format"); add("mp4")
            }
        }

        if (config.embedThumbnail) add("--embed-thumbnail")
        if (config.embedMetadata) add("--embed-metadata")
        if (config.embedChapters) add("--embed-chapters")
        if (config.sponsorblockRemove) { add("--sponsorblock-remove"); add("all") }
        if (config.restrictFilenames) add("--restrict-filenames")

        when (config.scope) {
            PlaylistScope.PLAYLIST -> {
                add("--yes-playlist")
                val items = config.playlistItems.trim()
                if (items.isNotEmpty()) { add("--playlist-items"); add(items) }
            }
            PlaylistScope.SINGLE -> add("--no-playlist")
        }

        if (cookiesPath != null) { add("--cookies"); add(cookiesPath) }

        val template =
            if (config.scope == PlaylistScope.PLAYLIST) TEMPLATE_PLAYLIST else TEMPLATE_SINGLE
        add("-o"); add("$outputDir/$template")
    }

    /** Build a [YoutubeDLRequest] for [url] with the args from [buildArgs]. */
    fun toRequest(
        config: DownloadConfig,
        url: String,
        outputDir: String,
        cookiesPath: String?,
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        // YoutubeDLRequest.addOption(String) appends a single token; feeding
        // flags and values one at a time keeps quoting/escaping out of our
        // hands (yt-dlp receives an argv array, not a shell string).
        buildArgs(config, outputDir, cookiesPath).forEach { request.addOption(it) }
        return request
    }
}
