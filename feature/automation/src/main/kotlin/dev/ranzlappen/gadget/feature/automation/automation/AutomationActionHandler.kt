package dev.ranzlappen.gadget.feature.automation.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.automation.R
import dev.ranzlappen.gadget.feature.automation.control.AutomationController
import dev.ranzlappen.gadget.feature.automation.control.AutomationControllerResult
import dev.ranzlappen.gadget.feature.automation.control.PrivilegedIntentConfig
import dev.ranzlappen.gadget.feature.automation.control.PrivilegedIntentVerb
import dev.ranzlappen.gadget.feature.automation.control.SystemSettingsOverrideConfig
import dev.ranzlappen.gadget.feature.automation.control.SystemSettingsScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `:feature:automation`'s invocable-action surface. This module is
 * deliberately screenless — its three capabilities (privileged intent
 * fire, allow-listed settings override, dumpsys snapshot) are generic
 * rooted power-user tools, not owned by any particular hardware feature.
 * Registering them here (rather than building a dedicated screen) is how
 * they become available as rule-builder actions in `:feature:automation-ui`,
 * which resolves every bound [ActionHandler] from the shared registry —
 * this is the resolution to the "confusing empty twin of automation-ui"
 * question (see wiki Completion-Master-Plan W2): the module isn't a
 * competing UI, it's the engine's own rooted-capability contributor.
 */
@Singleton
class AutomationActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: AutomationController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_FIRE_INTENT,
            label = context.getString(R.string.automation_action_fire_intent),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_VERB, ActionParamType.Text, PrivilegedIntentVerb.BROADCAST.name),
                ActionParam(PARAM_ACTION, ActionParamType.Text),
                ActionParam(PARAM_COMPONENT, ActionParamType.Text),
            ),
        ),
        ModuleAction(
            key = ACTION_OVERRIDE_SETTING,
            label = context.getString(R.string.automation_action_override_setting),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_SCOPE, ActionParamType.Text, SystemSettingsScope.SYSTEM.name),
                ActionParam(PARAM_KEY, ActionParamType.Text),
                ActionParam(PARAM_VALUE, ActionParamType.Text),
            ),
        ),
        ModuleAction(
            key = ACTION_DUMPSYS_SNAPSHOT,
            label = context.getString(R.string.automation_action_dumpsys_snapshot),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_RESET_ALL,
            label = context.getString(R.string.automation_action_reset_all),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_FIRE_INTENT -> {
                val verb = params[PARAM_VERB]?.let { runCatching { PrivilegedIntentVerb.valueOf(it) }.getOrNull() }
                    ?: PrivilegedIntentVerb.BROADCAST
                val action = params[PARAM_ACTION]?.takeIf { it.isNotBlank() }
                if (action == null) {
                    ActionResult.Failure("action is required")
                } else {
                    controller.firePrivilegedIntent(
                        PrivilegedIntentConfig(
                            verb = verb,
                            action = action,
                            componentFlatten = params[PARAM_COMPONENT]?.takeIf { it.isNotBlank() },
                        ),
                    ).toActionResult()
                }
            }
            ACTION_OVERRIDE_SETTING -> {
                val scope = params[PARAM_SCOPE]?.let { runCatching { SystemSettingsScope.valueOf(it) }.getOrNull() }
                val key = params[PARAM_KEY]?.takeIf { it.isNotBlank() }
                val value = params[PARAM_VALUE]
                if (scope == null || key == null || value == null) {
                    ActionResult.Failure("scope, key, and value are required")
                } else {
                    controller.overrideSystemSetting(
                        SystemSettingsOverrideConfig(scope = scope, key = key, value = value),
                    ).toActionResult()
                }
            }
            ACTION_DUMPSYS_SNAPSHOT -> controller.dumpsysSnapshot().toActionResult()
            ACTION_RESET_ALL -> controller.resetAllAutomationMutations().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun AutomationControllerResult.toActionResult(): ActionResult = when (this) {
        is AutomationControllerResult.Ok -> ActionResult.Success
        is AutomationControllerResult.ResetCompleted -> ActionResult.Success
        is AutomationControllerResult.IntentResult -> ActionResult.Success
        is AutomationControllerResult.DumpsysExcerpt -> ActionResult.Success
        AutomationControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        AutomationControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is AutomationControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is AutomationControllerResult.HardwareError -> ActionResult.Failure(message)
    }

    companion object {
        const val FEATURE_ID = "automation_extras"
        const val ACTION_FIRE_INTENT = "fire_privileged_intent"
        const val ACTION_OVERRIDE_SETTING = "override_system_setting"
        const val ACTION_DUMPSYS_SNAPSHOT = "dumpsys_snapshot"
        const val ACTION_RESET_ALL = "reset_all_mutations"
        const val PARAM_VERB = "verb"
        const val PARAM_ACTION = "action"
        const val PARAM_COMPONENT = "component"
        const val PARAM_SCOPE = "scope"
        const val PARAM_KEY = "key"
        const val PARAM_VALUE = "value"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AutomationExtrasActionModule {

    @Binds
    @IntoMap
    @StringKey(AutomationActionHandler.FEATURE_ID)
    fun bindAutomationActionHandler(handler: AutomationActionHandler): ActionHandler
}
