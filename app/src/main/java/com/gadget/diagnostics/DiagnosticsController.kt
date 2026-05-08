package com.gadget.diagnostics

/**
 * Rooted-only Diagnostics surface. Standard flavor returns
 * [DiagnosticsControllerResult.Unsupported] for every method.
 *
 * Privileged paths: `logcat -b <buffer> -d`, `dumpsys meminfo`,
 * `dumpsys cpuinfo`, `dumpsys procstats --hours 3`. All read-only,
 * tail-capped to 8 KB or 16 KB depending on output size. No auto-revert
 * path needed since this surface performs zero writes.
 */
interface DiagnosticsController {

    /**
     * Reads the tail of a logcat buffer. RADIO and SYSTEM contain
     * sensitive identifiers (IMSI fragments, GSM tower IDs); the helper
     * does not redact — the caller must opt in via [LogcatBuffer].
     */
    suspend fun tailLogcat(
        buffer: LogcatBuffer,
        persist: Boolean = false,
    ): DiagnosticsControllerResult

    /** Read-only `dumpsys meminfo` snapshot, tail-capped to 16 KB. */
    suspend fun dumpMemInfo(persist: Boolean = false): DiagnosticsControllerResult

    /** Read-only `dumpsys cpuinfo` snapshot, tail-capped to 8 KB. */
    suspend fun dumpCpuInfo(persist: Boolean = false): DiagnosticsControllerResult

    /**
     * Read-only `dumpsys procstats --hours 3` snapshot, tail-capped to
     * 16 KB. Heavier than the other dumps so its registry cap is
     * smaller.
     */
    suspend fun dumpProcstats(persist: Boolean = false): DiagnosticsControllerResult

    /**
     * No-op for the Diagnostics surface (zero writes), returns
     * [DiagnosticsControllerResult.ResetCompleted] with both counts at 0.
     * Kept for shape parity with the other Batch-7/8/9 controllers.
     */
    suspend fun resetAllDiagnosticsMutations(): DiagnosticsControllerResult

    /**
     * No-op for the Diagnostics surface. Kept for shape parity.
     */
    suspend fun revertOnScreenExit(): DiagnosticsControllerResult
}
