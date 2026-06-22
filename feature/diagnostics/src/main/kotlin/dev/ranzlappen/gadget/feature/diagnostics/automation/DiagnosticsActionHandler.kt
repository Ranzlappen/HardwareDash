package dev.ranzlappen.gadget.feature.diagnostics.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsActionHandler @Inject constructor() : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_TAIL_LOGCAT, label = "Tail logcat (500 lines)", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_MEMINFO, label = "Dump meminfo", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_CPUINFO, label = "Dump cpuinfo", requiresRoot = true),
        ModuleAction(key = ACTION_DUMP_PROCSTATS, label = "Dump procstats", requiresRoot = true),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        ActionResult.Unsupported

    companion object {
        const val FEATURE_ID = "diagnostics"
        const val ACTION_TAIL_LOGCAT = "diagnostics_tail_logcat"
        const val ACTION_DUMP_MEMINFO = "diagnostics_dump_meminfo"
        const val ACTION_DUMP_CPUINFO = "diagnostics_dump_cpuinfo"
        const val ACTION_DUMP_PROCSTATS = "diagnostics_dump_procstats"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(DiagnosticsActionHandler.FEATURE_ID)
    abstract fun bindDiagnosticsActionHandler(impl: DiagnosticsActionHandler): ActionHandler
}
