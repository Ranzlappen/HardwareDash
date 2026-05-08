package com.gadget.display

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val SURFACE_FLINGER_TAIL_CAP_BYTES = 8 * 1024

/**
 * Read-only `dumpsys SurfaceFlinger` snapshot. Tail-capped to 8 KB so
 * a runaway dump can't flood the shell buffer.
 */
@Singleton
class SurfaceFlingerDumpHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): String? {
        val cmd = "dumpsys SurfaceFlinger 2>/dev/null | tail -c $SURFACE_FLINGER_TAIL_CAP_BYTES"
        val result = shell.exec(cmd)
        if (!result.isSuccess) return null
        return result.stdout.joinToString("\n").take(SURFACE_FLINGER_TAIL_CAP_BYTES)
    }
}
