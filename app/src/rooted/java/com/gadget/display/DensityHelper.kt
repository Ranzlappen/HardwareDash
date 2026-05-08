package com.gadget.display

import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val DENSITY_MIN_DPI = 120
internal const val DENSITY_MAX_DPI = 560

/**
 * Runtime DPI override via `wm density <dpi>`. The [dpi] value is
 * clamped to 120–560 inside the helper regardless of caller input.
 * Revert is `wm density reset` — the canonical undo path; the helper
 * also stores the reported original dpi (when available) in the
 * mutation log under `cmd-display://wm/density` so the snapshot is
 * recoverable across process kill.
 */
@Singleton
class DensityHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun overrideDensity(dpi: Int): DensityOutcome {
        val clampedDpi = dpi.coerceIn(DENSITY_MIN_DPI, DENSITY_MAX_DPI)
        val original = readPhysicalDensity()
        val pseudoPath = "cmd-display://wm/density"
        mutationLog.register(pseudoPath, original?.toString() ?: "reset")
        val write = shell.exec("wm density $clampedDpi")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return DensityOutcome.WriteFailed
        }
        return DensityOutcome.Applied(originalDpi = original, appliedDpi = clampedDpi)
    }

    suspend fun resetDensity(): Boolean {
        val result = shell.exec("wm density reset")
        if (result.isSuccess) {
            mutationLog.unregister("cmd-display://wm/density")
        }
        return result.isSuccess
    }

    private suspend fun readPhysicalDensity(): Int? {
        val result = shell.exec("wm density")
        if (!result.isSuccess) return null
        val raw = result.stdout.joinToString("\n")
        return Regex("Physical density:\\s*(\\d+)").find(raw)?.groupValues?.get(1)?.toIntOrNull()
    }
}

sealed class DensityOutcome {
    data object WriteFailed : DensityOutcome()
    data class Applied(val originalDpi: Int?, val appliedDpi: Int) : DensityOutcome()
}
