package dev.ranzlappen.gadget.feature.diagnostics.rooted

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootedDiagnosticsActionHandler @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_TAIL_LOGCAT, label = "Tail logcat (500 lines, auto-redacted)", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_MEMINFO, label = "Dump meminfo (8 KB cap)", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_CPUINFO, label = "Dump cpuinfo (8 KB cap)", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_PROCSTATS, label = "Dump procstats (8 KB cap)", requiresRoot = true),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_TAIL_LOGCAT -> runGated(RootFeatureKey.DiagnosticsTailLogcat) {
                val result = shell.exec(
                    "logcat -d -b all -t 500 2>/dev/null | grep -v 'RADIO\\|SYSTEM_PRIVATE'",
                    timeoutMillis = 15_000,
                )
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("logcat failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_DUMP_MEMINFO -> runGated(RootFeatureKey.DiagnosticsDumpMemInfo) {
                val result = shell.exec("dumpsys meminfo", timeoutMillis = 10_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("meminfo failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_DUMP_CPUINFO -> runGated(RootFeatureKey.DiagnosticsDumpCpuInfo) {
                val result = shell.exec("dumpsys cpuinfo", timeoutMillis = 10_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("cpuinfo failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            ACTION_DUMP_PROCSTATS -> runGated(RootFeatureKey.DiagnosticsDumpProcstats) {
                val result = shell.exec("dumpsys procstats", timeoutMillis = 15_000)
                if (result.isSuccess) ActionResult.Success
                else ActionResult.Failure("procstats failed: ${result.stderr.firstOrNull().orEmpty()}")
            }
            else -> ActionResult.Unsupported
        }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> ActionResult,
    ): ActionResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is ActionResult.Success) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> ActionResult.Failure("Blocked by user preference")
        is RootGateDecision.BlockedByLimiter ->
            ActionResult.Failure("Rate limited; retry after ${gate.retryAfterMillis}ms")
        RootGateDecision.Unsupported -> ActionResult.Unsupported
    }

    companion object {
        const val FEATURE_ID = "diagnostics_root"
        const val ACTION_TAIL_LOGCAT = "diagnostics_root_tail_logcat"
        const val ACTION_DUMP_MEMINFO = "diagnostics_root_dump_meminfo"
        const val ACTION_DUMP_CPUINFO = "diagnostics_root_dump_cpuinfo"
        const val ACTION_DUMP_PROCSTATS = "diagnostics_root_dump_procstats"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootedDiagnosticsActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(RootedDiagnosticsActionHandler.FEATURE_ID)
    abstract fun bindRootedDiagnosticsActionHandler(impl: RootedDiagnosticsActionHandler): ActionHandler
}
