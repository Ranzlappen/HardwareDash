package com.gadget.audio

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val AUDIO_DUMP_TAIL_CAP_BYTES = 8 * 1024

/**
 * Read-only `cmd audio dump` snapshot. Tail-capped to 8 KB so a runaway
 * dump can't flood the shell buffer.
 */
@Singleton
class AudioDumpHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): String? {
        val cmd = "cmd audio dump 2>/dev/null | tail -c $AUDIO_DUMP_TAIL_CAP_BYTES"
        val result = shell.exec(cmd)
        if (!result.isSuccess) return null
        return result.stdout.joinToString("\n").take(AUDIO_DUMP_TAIL_CAP_BYTES)
    }
}
