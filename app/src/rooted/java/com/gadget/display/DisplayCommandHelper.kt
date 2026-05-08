package com.gadget.display

import android.os.Build
import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val REFRESH_RATE_HARD_HZ_CEILING = 165

/**
 * `cmd display` shell-out helper. Used for refresh-rate overrides only —
 * `cmd display set-display-mode` arrived in API 30 (Android 11) so the
 * helper guards on [Build.VERSION_CODES.R] and surfaces "unavailable"
 * on lower SDKs.
 *
 * Snapshots the active mode id via `cmd display get-active-display-mode`
 * before the override write so screen-exit revert is reliable.
 */
@Singleton
class DisplayCommandHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    fun isApiSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    suspend fun activeModeId(displayId: Int): Int? {
        if (!isApiSupported()) return null
        val result = shell.exec("cmd display get-active-display-mode $displayId 2>/dev/null")
        if (!result.isSuccess) return null
        val raw = result.stdout.joinToString("\n")
        return Regex("id\\s*=\\s*(\\d+)").find(raw)?.groupValues?.get(1)?.toIntOrNull()
    }

    suspend fun listModes(displayId: Int): List<DisplayMode> {
        if (!isApiSupported()) return emptyList()
        val result = shell.exec("cmd display list-display-modes $displayId 2>/dev/null")
        if (!result.isSuccess) return emptyList()
        return result.stdout.mapNotNull(::parseModeLine)
    }

    suspend fun setDisplayMode(displayId: Int, modeId: Int): SetModeOutcome {
        if (!isApiSupported()) return SetModeOutcome.Unsupported
        val modes = listModes(displayId)
        val target = modes.firstOrNull { it.id == modeId }
            ?: return SetModeOutcome.WriteFailed("mode $modeId not in list-display-modes")
        if (target.refreshHz > REFRESH_RATE_HARD_HZ_CEILING) {
            return SetModeOutcome.WriteFailed(
                "${target.refreshHz} Hz exceeds hard cap of $REFRESH_RATE_HARD_HZ_CEILING Hz",
            )
        }
        val original = activeModeId(displayId)
            ?: return SetModeOutcome.WriteFailed("could not read active mode")
        val pseudoPath = "cmd-display://display/$displayId/mode"
        mutationLog.register(pseudoPath, original.toString())
        val result = shell.exec("cmd display set-display-mode $displayId $modeId")
        if (!result.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return SetModeOutcome.WriteFailed("cmd display set-display-mode rejected the write")
        }
        return SetModeOutcome.Applied(originalModeId = original, appliedModeId = modeId)
    }

    private fun parseModeLine(line: String): DisplayMode? {
        val idMatch = Regex("id\\s*=\\s*(\\d+)").find(line) ?: return null
        val refreshMatch = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:Hz|fps)").find(line) ?: return null
        val id = idMatch.groupValues[1].toIntOrNull() ?: return null
        val hz = refreshMatch.groupValues[1].toFloatOrNull()?.toInt() ?: return null
        return DisplayMode(id = id, refreshHz = hz)
    }
}

data class DisplayMode(val id: Int, val refreshHz: Int)

sealed class SetModeOutcome {
    data object Unsupported : SetModeOutcome()
    data class Applied(val originalModeId: Int, val appliedModeId: Int) : SetModeOutcome()
    data class WriteFailed(val message: String) : SetModeOutcome()
}
