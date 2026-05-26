package dev.ranzlappen.gadget.core.monitoring

/**
 * Shared downsampling math for the monitor chart + chart widget. Collapsing a
 * window into fixed time buckets is what keeps a long window (up to 24h) from
 * feeding tens of thousands of points to the renderer — the bucket count is
 * the cap regardless of poll rate or window length.
 */
object MonitorDownsampling {

    /** In-app chart point cap (a bucket spans `window / IN_APP_MAX_POINTS`). */
    const val IN_APP_MAX_POINTS = 500L

    /**
     * The bucket width (ms) for a [windowMs] window: small enough that
     * `window / bucketMs <= maxPoints`, but never finer than [pollIntervalMs]
     * (no point sub-sampling below the sample rate) and never below 1ms (the
     * SQL bucket divisor must be `>= 1`).
     */
    fun bucketMs(windowMs: Long, pollIntervalMs: Long, maxPoints: Long): Long {
        val cap = maxPoints.coerceAtLeast(1L)
        val byCap = (windowMs + cap - 1) / cap // ceil(windowMs / cap)
        return maxOf(byCap, pollIntervalMs, 1L)
    }
}
