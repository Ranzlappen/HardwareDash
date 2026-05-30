package com.gadget.diagnostics

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor Diagnostics controller. Wires the safety gate to the
 * four read-only diagnostics helpers. No auto-revert path needed since
 * this surface performs zero writes; the screen-exit method is a no-op
 * kept for shape parity with the other Batch-7/8/9 controllers.
 */
@Singleton
class RootedDiagnosticsController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val logcatHelper: LogcatTailHelper,
    private val memInfoHelper: MemInfoDumpHelper,
    private val cpuInfoHelper: CpuInfoDumpHelper,
    private val procstatsHelper: ProcstatsDumpHelper,
) : DiagnosticsController {

    override suspend fun tailLogcat(
        buffer: LogcatBuffer,
        persist: Boolean,
    ): DiagnosticsControllerResult =
        runGated(RootFeatureKey.DiagnosticsTailLogcat) {
            val excerpt = logcatHelper.snapshot(buffer)
                ?: return@runGated DiagnosticsControllerResult.HardwareError(
                    "logcat -b ${buffer.wireName} failed",
                )
            val persistedFile = if (persist) {
                logcatHelper.persistToLogbook(buffer, excerpt)?.absolutePath
            } else {
                null
            }
            DiagnosticsControllerResult.LogcatExcerpt(
                buffer = buffer,
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun dumpMemInfo(persist: Boolean): DiagnosticsControllerResult =
        runGated(RootFeatureKey.DiagnosticsDumpMemInfo) {
            val excerpt = memInfoHelper.snapshot()
                ?: return@runGated DiagnosticsControllerResult.HardwareError(
                    "dumpsys meminfo failed",
                )
            val persistedFile = if (persist) {
                memInfoHelper.persistToLogbook(excerpt)?.absolutePath
            } else {
                null
            }
            DiagnosticsControllerResult.MemInfoExcerpt(
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun dumpCpuInfo(persist: Boolean): DiagnosticsControllerResult =
        runGated(RootFeatureKey.DiagnosticsDumpCpuInfo) {
            val excerpt = cpuInfoHelper.snapshot()
                ?: return@runGated DiagnosticsControllerResult.HardwareError(
                    "dumpsys cpuinfo failed",
                )
            val persistedFile = if (persist) {
                cpuInfoHelper.persistToLogbook(excerpt)?.absolutePath
            } else {
                null
            }
            DiagnosticsControllerResult.CpuInfoExcerpt(
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun dumpProcstats(persist: Boolean): DiagnosticsControllerResult =
        runGated(RootFeatureKey.DiagnosticsDumpProcstats) {
            val excerpt = procstatsHelper.snapshot()
                ?: return@runGated DiagnosticsControllerResult.HardwareError(
                    "dumpsys procstats failed",
                )
            val persistedFile = if (persist) {
                procstatsHelper.persistToLogbook(excerpt)?.absolutePath
            } else {
                null
            }
            DiagnosticsControllerResult.ProcstatsExcerpt(
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun resetAllDiagnosticsMutations(): DiagnosticsControllerResult =
        DiagnosticsControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): DiagnosticsControllerResult =
        DiagnosticsControllerResult.ResetCompleted(restored = 0, failed = 0)

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> DiagnosticsControllerResult,
    ): DiagnosticsControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it !is DiagnosticsControllerResult.OptedOut &&
                it !is DiagnosticsControllerResult.Unsupported &&
                it !is DiagnosticsControllerResult.RateLimited &&
                it !is DiagnosticsControllerResult.HardwareError
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> DiagnosticsControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            DiagnosticsControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> DiagnosticsControllerResult.Unsupported
    }
}
