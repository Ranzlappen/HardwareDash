package dev.ranzlappen.gadget.feature.ambient.widget

import kotlin.math.log10

/**
 * Pure lux → gauge helpers for the widget. Lux spans ~0 (dark) to >100k (direct
 * sun), so the bar uses a log scale. Kept Android-free so the mapping and the
 * level bucketing round-trip in a plain JVM test.
 */
object AmbientBrightness {

    /** Coarse brightness buckets, matching the in-app level descriptors. */
    enum class Level { Dark, Dim, Indoor, Bright, Sunlight }

    private const val FULL_PERCENT = 100

    /** log10 of the top of the displayed range (≈100k lux ≈ direct sunlight). */
    private const val LOG_MAX = 5.0

    /**
     * Map a lux reading to a 0–100 gauge percent on a log scale (so the
     * common 10–1000 lux indoor band fills a readable middle, not a sliver).
     */
    fun brightnessPercent(lux: Float): Int {
        if (lux <= 0f) return 0
        val pct = (log10(lux.toDouble() + 1.0) / LOG_MAX * FULL_PERCENT).toInt()
        return pct.coerceIn(0, FULL_PERCENT)
    }

    /** Bucket a lux reading into a coarse [Level] descriptor. */
    fun level(lux: Float): Level = when {
        lux < 10f -> Level.Dark
        lux < 100f -> Level.Dim
        lux < 1_000f -> Level.Indoor
        lux < 10_000f -> Level.Bright
        else -> Level.Sunlight
    }
}
