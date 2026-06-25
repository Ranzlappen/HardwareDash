package dev.ranzlappen.gadget.feature.youtubedownloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure yt-dlp argument builder — the Kotlin port of the
 * tools repo's `youtube-mp3/tool.js` `buildTokens()`.
 */
class YtDlpRequestBuilderTest {

    private val outDir = "/storage/emulated/0/Android/data/app/files/Movies"

    /** Assert that [flag] is immediately followed by [value] in the arg list. */
    private fun assertPair(args: List<String>, flag: String, value: String) {
        val i = args.indexOf(flag)
        assertTrue("expected flag $flag present", i >= 0)
        assertEquals("expected $flag to be followed by $value", value, args[i + 1])
    }

    @Test
    fun audioMp3_emitsExtractAndQuality() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(
                url = "u",
                kind = MediaKind.AUDIO,
                audioFormat = AudioFormat.MP3,
                audioQuality = AudioQuality.Q320,
            ),
            outDir,
            cookiesPath = null,
        )
        assertTrue(args.contains("-x"))
        assertPair(args, "--audio-format", "mp3")
        assertPair(args, "--audio-quality", "320K")
        assertPair(args, "-f", "bestaudio/best")
    }

    @Test
    fun audioNonMp3_omitsBitrate() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(kind = MediaKind.AUDIO, audioFormat = AudioFormat.OPUS),
            outDir,
            cookiesPath = null,
        )
        assertFalse(args.contains("--audio-quality"))
        assertPair(args, "--audio-format", "opus")
    }

    @Test
    fun video_emitsFormatSelectorAndMp4Merge() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(kind = MediaKind.VIDEO, videoQuality = VideoQuality.P1080),
            outDir,
            cookiesPath = null,
        )
        assertPair(args, "-f", VideoQuality.P1080.formatSelector)
        assertPair(args, "--merge-output-format", "mp4")
    }

    @Test
    fun playlistScope_withItems_emitsRange() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(scope = PlaylistScope.PLAYLIST, playlistItems = "1-10,15"),
            outDir,
            cookiesPath = null,
        )
        assertTrue(args.contains("--yes-playlist"))
        assertPair(args, "--playlist-items", "1-10,15")
        assertPair(args, "-o", "$outDir/${YtDlpRequestBuilder.TEMPLATE_PLAYLIST}")
    }

    @Test
    fun singleScope_emitsNoPlaylistAndSingleTemplate() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(scope = PlaylistScope.SINGLE),
            outDir,
            cookiesPath = null,
        )
        assertTrue(args.contains("--no-playlist"))
        assertPair(args, "-o", "$outDir/${YtDlpRequestBuilder.TEMPLATE_SINGLE}")
    }

    @Test
    fun cookiesPath_whenPresent_emitsCookiesFlag() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(useCookies = true),
            outDir,
            cookiesPath = "/data/cookies.txt",
        )
        assertPair(args, "--cookies", "/data/cookies.txt")
    }

    @Test
    fun cookiesPath_whenNull_omitsCookiesFlag() {
        val args = YtDlpRequestBuilder.buildArgs(DownloadConfig(), outDir, cookiesPath = null)
        assertFalse(args.contains("--cookies"))
    }

    @Test
    fun embedFlags_followToggles() {
        val args = YtDlpRequestBuilder.buildArgs(
            DownloadConfig(
                embedThumbnail = true,
                embedMetadata = true,
                embedChapters = true,
                sponsorblockRemove = true,
                restrictFilenames = true,
            ),
            outDir,
            cookiesPath = null,
        )
        assertTrue(args.contains("--embed-thumbnail"))
        assertTrue(args.contains("--embed-metadata"))
        assertTrue(args.contains("--embed-chapters"))
        assertPair(args, "--sponsorblock-remove", "all")
        assertTrue(args.contains("--restrict-filenames"))
    }
}
