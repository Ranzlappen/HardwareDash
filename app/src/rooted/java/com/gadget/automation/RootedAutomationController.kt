package com.gadget.automation

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val AUTOMATION_RESET_PREFIXES = listOf("settings://")
private val AUTOMATION_SCREEN_EXIT_PREFIXES = listOf("settings://")

@Singleton
class RootedAutomationController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val intentHelper: PrivilegedIntentHelper,
    private val settingsHelper: SystemSettingsHelper,
    private val dumpsysHelper: DumpsysHelper,
    private val mutationLog: SysfsMutationLog,
) : AutomationController {

    override suspend fun firePrivilegedIntent(
        config: PrivilegedIntentConfig,
    ): AutomationControllerResult =
        runGated(RootFeatureKey.AutomationPrivilegedIntent) { intentHelper.fire(config) }

    override suspend fun overrideSystemSetting(
        config: SystemSettingsOverrideConfig,
    ): AutomationControllerResult =
        runGated(RootFeatureKey.AutomationSystemSettingsOverride) { settingsHelper.put(config) }

    override suspend fun dumpsysSnapshot(): AutomationControllerResult =
        runGated(RootFeatureKey.AutomationDumpsysSnapshot) {
            AutomationControllerResult.DumpsysExcerpt(dumpsysHelper.snapshot())
        }

    override suspend fun resetAllAutomationMutations(): AutomationControllerResult {
        val outcome = mutationLog.revertAll(AUTOMATION_RESET_PREFIXES)
        return AutomationControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): AutomationControllerResult {
        val outcome = mutationLog.revertAll(AUTOMATION_SCREEN_EXIT_PREFIXES)
        return AutomationControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> AutomationControllerResult,
    ): AutomationControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is AutomationControllerResult.Ok ||
                it is AutomationControllerResult.IntentResult ||
                it is AutomationControllerResult.DumpsysExcerpt
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> AutomationControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            AutomationControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> AutomationControllerResult.Unsupported
    }
}
