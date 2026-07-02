package dev.ranzlappen.gadget.feature.diagnostics

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System RAM-in-use metric — the **standard-flavor** memory signal.
 *
 * Reads `ActivityManager.MemoryInfo` (`totalMem` / `availMem`), which every
 * app can query with no permission and no root, and reports the percentage
 * of physical RAM currently in use. This is the un-privileged counterpart to
 * the rooted `/proc/meminfo` capability the diagnostics screen lists — it
 * gives the monitoring framework a chartable, alertable memory signal and
 * the automation engine a memory trigger source without needing root.
 *
 * Poll source (no [stream]): RAM pressure changes continuously, so a regular
 * sample on the monitor's cadence is the right cost model — there's no
 * discrete "changed" event to push.
 */
@Singleton
class MemoryMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Memory in use",
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return usedPercent(totalBytes = info.totalMem, availBytes = info.availMem)
    }

    companion object {
        const val METRIC_KEY = "memory_used_percent"

        /**
         * Percentage of physical RAM in use, clamped to `0..100`.
         *
         * `used = total - avail`, guarded so a transient `avail > total` (or a
         * zero/negative `total` on an unusual device) can never produce a
         * negative or out-of-range reading. Pure + side-effect-free so it's
         * unit-testable without an `ActivityManager`.
         */
        fun usedPercent(totalBytes: Long, availBytes: Long): Float {
            if (totalBytes <= 0L) return 0f
            val used = (totalBytes - availBytes).coerceIn(0L, totalBytes)
            return (used.toDouble() / totalBytes.toDouble() * 100.0).toFloat()
        }
    }
}
