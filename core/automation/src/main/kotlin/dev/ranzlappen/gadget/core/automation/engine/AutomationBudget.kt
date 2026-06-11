package dev.ranzlappen.gadget.core.automation.engine

/**
 * Storm budget for the runtime (ADR-0002 Decision 8): caps how many actions
 * the engine dispatches so chaining (one rule's action moving a metric that
 * is another rule's trigger) can't spin into a runaway.
 *
 * Two caps, both enforced by [admit]:
 *  - **per-cycle** — at most [maxPerCycle] dispatches admitted in a single
 *    trigger-evaluation cycle (one `admit` call = one cycle).
 *  - **rolling** — at most [maxPerWindow] dispatches in any [windowMs]
 *    sliding window.
 *
 * Pure + clock-injected (the caller passes `now`), so the whole policy is
 * JVM-unit-testable with no Android. **Not** thread-safe — the runtime calls
 * it from a single structured-concurrency cycle; if that ever changes, guard
 * it with the service's existing mutex.
 *
 * On breach the overflow is **dropped** (not queued) and [Admission.throttled]
 * is set so the runtime can post a single "Automation throttled" notification
 * instead of silently spinning.
 */
class AutomationBudget(
    private val maxPerCycle: Int = DEFAULT_MAX_PER_CYCLE,
    private val maxPerWindow: Int = DEFAULT_MAX_PER_WINDOW,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
) {
    // Timestamps of admitted dispatches within the current rolling window.
    // Ordered oldest-first; pruned on each admit. Bounded by maxPerWindow.
    private val recent = ArrayDeque<Long>()

    /** Outcome of one cycle: how many to dispatch + whether anything was dropped. */
    data class Admission(val allowed: Int, val throttled: Boolean) {
        init {
            require(allowed >= 0) { "allowed must be >= 0, was $allowed" }
        }
    }

    /**
     * Admit up to [requested] dispatches at time [now] (epoch ms). Returns
     * the number permitted under both caps and whether any were dropped.
     * Records the admitted count against the rolling window.
     */
    fun admit(now: Long, requested: Int): Admission {
        require(requested >= 0) { "requested must be >= 0, was $requested" }
        if (requested == 0) return Admission(allowed = 0, throttled = false)

        pruneOlderThan(now - windowMs)
        val windowRemaining = (maxPerWindow - recent.size).coerceAtLeast(0)
        val allowed = minOf(requested, maxPerCycle, windowRemaining)
        repeat(allowed) { recent.addLast(now) }
        return Admission(allowed = allowed, throttled = allowed < requested)
    }

    private fun pruneOlderThan(cutoffExclusive: Long) {
        // A timestamp exactly windowMs old has aged out (window is the last
        // windowMs, i.e. `> now - windowMs`).
        while (recent.isNotEmpty() && recent.first() <= cutoffExclusive) {
            recent.removeFirst()
        }
    }

    companion object {
        const val DEFAULT_MAX_PER_CYCLE = 16
        const val DEFAULT_MAX_PER_WINDOW = 60
        const val DEFAULT_WINDOW_MS = 60_000L
    }
}
