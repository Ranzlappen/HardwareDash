package dev.ranzlappen.gadget.feature.diagnostics.control

/**
 * Result returned by every [DiagnosticsController] privileged method.
 * Same shape as the Batch-7 / Batch-8 / Batch-9 controller result types.
 */
sealed class DiagnosticsControllerResult {
    data class Ok(val statusNote: String? = null) : DiagnosticsControllerResult()
    data object Unsupported : DiagnosticsControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : DiagnosticsControllerResult()
    data object OptedOut : DiagnosticsControllerResult()
    data class HardwareError(val message: String) : DiagnosticsControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : DiagnosticsControllerResult()

    /**
     * Logcat buffer excerpt. [persistedFile] is the absolute path of the
     * JSON snapshot if the caller passed `persist = true` and the
     * Logbook write succeeded.
     */
    data class LogcatExcerpt(
        val buffer: LogcatBuffer,
        val excerpt: String,
        val persistedFile: String? = null,
    ) : DiagnosticsControllerResult()

    /** `dumpsys meminfo` excerpt. */
    data class MemInfoExcerpt(
        val excerpt: String,
        val persistedFile: String? = null,
    ) : DiagnosticsControllerResult()

    /** `dumpsys cpuinfo` excerpt. */
    data class CpuInfoExcerpt(
        val excerpt: String,
        val persistedFile: String? = null,
    ) : DiagnosticsControllerResult()

    /** `dumpsys procstats --hours 3` excerpt. */
    data class ProcstatsExcerpt(
        val excerpt: String,
        val persistedFile: String? = null,
    ) : DiagnosticsControllerResult()
}
