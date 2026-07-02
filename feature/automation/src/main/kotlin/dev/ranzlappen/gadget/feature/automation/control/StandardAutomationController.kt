package dev.ranzlappen.gadget.feature.automation.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Automation controller. Every privileged method
 * returns [AutomationControllerResult.Unsupported].
 */
@Singleton
class StandardAutomationController @Inject constructor() : AutomationController {

    override suspend fun firePrivilegedIntent(
        config: PrivilegedIntentConfig,
    ): AutomationControllerResult = AutomationControllerResult.Unsupported

    override suspend fun overrideSystemSetting(
        config: SystemSettingsOverrideConfig,
    ): AutomationControllerResult = AutomationControllerResult.Unsupported

    override suspend fun dumpsysSnapshot(): AutomationControllerResult =
        AutomationControllerResult.Unsupported

    override suspend fun resetAllAutomationMutations(): AutomationControllerResult =
        AutomationControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): AutomationControllerResult =
        AutomationControllerResult.ResetCompleted(restored = 0, failed = 0)
}
