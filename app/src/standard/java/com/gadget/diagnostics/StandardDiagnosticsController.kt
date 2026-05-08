package com.gadget.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Diagnostics controller. Every method returns
 * [DiagnosticsControllerResult.Unsupported] — the standard APK has no
 * privileged shell so direct `logcat -b radio` and `dumpsys` reads of
 * protected services are impossible regardless of permissions.
 */
@Singleton
class StandardDiagnosticsController @Inject constructor() : DiagnosticsController {

    override suspend fun tailLogcat(
        buffer: LogcatBuffer,
        persist: Boolean,
    ): DiagnosticsControllerResult = DiagnosticsControllerResult.Unsupported

    override suspend fun dumpMemInfo(persist: Boolean): DiagnosticsControllerResult =
        DiagnosticsControllerResult.Unsupported

    override suspend fun dumpCpuInfo(persist: Boolean): DiagnosticsControllerResult =
        DiagnosticsControllerResult.Unsupported

    override suspend fun dumpProcstats(persist: Boolean): DiagnosticsControllerResult =
        DiagnosticsControllerResult.Unsupported

    override suspend fun resetAllDiagnosticsMutations(): DiagnosticsControllerResult =
        DiagnosticsControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): DiagnosticsControllerResult =
        DiagnosticsControllerResult.ResetCompleted(restored = 0, failed = 0)
}
