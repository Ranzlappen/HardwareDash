package dev.ranzlappen.gadget.feature.display.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal const val BACKLIGHT_OVERDRIVE_PERCENT = 130
internal const val BACKLIGHT_HARD_CEILING_MILLIS = 60_000L
private const val BACKLIGHT_SCALE_DIVISOR = 100

private val BACKLIGHT_PATH_CANDIDATES = listOf(
    "/sys/class/leds/lcd-backlight",
    "/sys/class/leds/lcd_backlight",
    "/sys/class/leds/wled",
    "/sys/class/backlight/panel0-backlight",
)

/**
 * Direct LCD-backlight sysfs writer. Reads `max_brightness` per device
 * and clamps the effective raw value to 130 % of that ceiling
 * (panels accept values above max_brightness on most Snapdragon /
 * Tensor devices; the 130 % cap is our soft thermal-headroom limit).
 *
 * Snapshot+restore via the shared mutation log under
 * `sysfs-backlight://<path>/brightness`. The active window is
 * hard-capped to 60 s via [withTimeoutOrNull]; the original value is
 * always restored in a `NonCancellable` finally even if the caller is
 * cancelled.
 */
@Singleton
class BacklightSysfsHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun resolveBasePath(): String? {
        for (candidate in BACKLIGHT_PATH_CANDIDATES) {
            val probe = shell.exec("test -d $candidate && echo ok")
            if (probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok") {
                return candidate
            }
        }
        return null
    }

    suspend fun overrideBrightness(
        percent: Int,
        activeWindowMillis: Long,
    ): BrightnessOutcome {
        val basePath = resolveBasePath() ?: return BrightnessOutcome.Unavailable
        val maxBrightness = readInt("$basePath/max_brightness")
            ?: return BrightnessOutcome.Unavailable
        val original = readInt("$basePath/brightness")
            ?: return BrightnessOutcome.Unavailable
        val hardCeiling = (maxBrightness * BACKLIGHT_OVERDRIVE_PERCENT) / BACKLIGHT_SCALE_DIVISOR
        val requestedRaw = (maxBrightness * percent.coerceAtLeast(0)) / BACKLIGHT_SCALE_DIVISOR
        val appliedRaw = requestedRaw.coerceAtMost(hardCeiling)
        val pseudoPath = "sysfs-backlight://$basePath/brightness"
        val effectiveWindow = activeWindowMillis.coerceAtMost(BACKLIGHT_HARD_CEILING_MILLIS)
        mutationLog.register(pseudoPath, original.toString())
        val write = shell.exec("echo $appliedRaw > $basePath/brightness")
        if (!write.isSuccess) {
            mutationLog.unregister(pseudoPath)
            return BrightnessOutcome.WriteFailed
        }
        try {
            withTimeoutOrNull(effectiveWindow) { delay(effectiveWindow) }
        } finally {
            withContext(NonCancellable) {
                shell.exec("echo $original > $basePath/brightness")
                mutationLog.unregister(pseudoPath)
            }
        }
        return BrightnessOutcome.Applied(
            originalRaw = original,
            appliedRaw = appliedRaw,
            maxBrightness = maxBrightness,
        )
    }

    private suspend fun readInt(path: String): Int? {
        val result = shell.exec("cat $path")
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()?.toIntOrNull()
    }
}

sealed class BrightnessOutcome {
    data object Unavailable : BrightnessOutcome()
    data object WriteFailed : BrightnessOutcome()
    data class Applied(
        val originalRaw: Int,
        val appliedRaw: Int,
        val maxBrightness: Int,
    ) : BrightnessOutcome()
}
