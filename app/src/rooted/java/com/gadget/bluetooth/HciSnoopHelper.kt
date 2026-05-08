package com.gadget.bluetooth

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val SNOOP_LOG_PRIMARY = "/data/misc/bluetooth/logs/btsnoop_hci.log"
private const val SNOOP_LOG_FALLBACK = "/sdcard/btsnoop_hci.log"
private const val TAIL_LINE_LIMIT = 64

/**
 * Read-only tail of the HCI snoop log if the user has enabled
 * developer-options HCI snoop logging. Surfaces the last
 * [TAIL_LINE_LIMIT] hex-dumped lines (best-effort: snoop logs are
 * binary, so we tail them as text and let the caller treat the result
 * as opaque diagnostic output).
 */
@Singleton
class HciSnoopHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun tail(): List<String>? {
        val candidates = listOf(SNOOP_LOG_PRIMARY, SNOOP_LOG_FALLBACK)
        for (path in candidates) {
            val probe = shell.exec("test -r \"$path\" && echo ok")
            if (!probe.isSuccess) continue
            if (probe.stdout.firstOrNull()?.trim() != "ok") continue
            val read = shell.exec("tail -n $TAIL_LINE_LIMIT \"$path\" | od -An -c | head -n $TAIL_LINE_LIMIT")
            if (read.isSuccess) return read.stdout
        }
        return null
    }
}
