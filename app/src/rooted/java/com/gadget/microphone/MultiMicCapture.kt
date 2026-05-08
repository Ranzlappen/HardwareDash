package com.gadget.microphone

import com.gadget.root.core.RootShell
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

internal const val MULTI_MIC_HARD_CEILING_MILLIS = 30_000L
internal const val MULTI_MIC_DEFAULT_MAX_STREAMS = 3
private const val SHELL_TIMEOUT_MARGIN_MS = 2_000L

/**
 * Captures from every available input PCM node concurrently. Each stream
 * is driven by its own `tinycap` shell invocation; outputs are discarded
 * to `/dev/null` since this capability is for "exercise the mic
 * subsystem" rather than recording.
 *
 * Hard 30 s per-session ceiling and max 3 concurrent streams (default).
 */
@Singleton
class MultiMicCapture @Inject constructor(
    private val shell: RootShell,
    private val paths: MicSysfsPaths,
) {
    suspend fun capture(config: MultiMicConfig): MicrophoneControllerResult = coroutineScope {
        val surface = paths.resolve()
        val streams = surface.captureDevices.take(config.maxStreams.coerceAtMost(MULTI_MIC_DEFAULT_MAX_STREAMS))
        if (streams.isEmpty()) return@coroutineScope MicrophoneControllerResult.Unsupported
        val effectiveDuration = config.durationMillis.coerceAtMost(MULTI_MIC_HARD_CEILING_MILLIS)

        val results = streams.map { node ->
            async { captureOne(node, effectiveDuration) }
        }.awaitAll()

        val anyOk = results.any { it }
        if (anyOk) MicrophoneControllerResult.Ok else MicrophoneControllerResult.HardwareError(
            "all ${streams.size} mic streams failed",
        )
    }

    private suspend fun captureOne(node: String, durationMillis: Long): Boolean {
        val match = Regex("pcmC(\\d+)D(\\d+)c").find(node) ?: return false
        val card = match.groupValues[1].toInt()
        val device = match.groupValues[2].toInt()
        val seconds = (durationMillis / 1000).coerceAtLeast(1L)
        val script = "tinycap /dev/null -D $card -d $device -T $seconds"
        val result = shell.exec(script, timeoutMillis = durationMillis + SHELL_TIMEOUT_MARGIN_MS)
        return result.isSuccess
    }
}
