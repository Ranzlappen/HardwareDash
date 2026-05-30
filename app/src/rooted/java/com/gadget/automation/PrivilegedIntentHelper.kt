package com.gadget.automation

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private val ACTION_DENY_FRAGMENTS = listOf(
    "REBOOT",
    "SHUTDOWN",
    "FACTORY_RESET",
    "MASTER_CLEAR",
)
private val COMPONENT_DENY_FRAGMENTS = listOf(
    "com.android.systemui/.recents",
    "com.android.systemui/.screenshot",
)
private const val MAX_TAIL_CHARS = 4_096

/**
 * Wraps `am broadcast` / `am start` / `am start-service`. Enforces a
 * caller-independent action deny-list before any shell exec.
 */
@Singleton
class PrivilegedIntentHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun fire(config: PrivilegedIntentConfig): AutomationControllerResult {
        val action = config.action
        if (ACTION_DENY_FRAGMENTS.any { action.uppercase().contains(it) }) {
            return AutomationControllerResult.HardwareError("action $action is on the deny-list")
        }
        val component = config.componentFlatten
        if (component != null && COMPONENT_DENY_FRAGMENTS.any { component.contains(it) }) {
            return AutomationControllerResult.HardwareError("component $component is on the deny-list")
        }
        val verb = when (config.verb) {
            PrivilegedIntentVerb.BROADCAST -> "broadcast"
            PrivilegedIntentVerb.START_ACTIVITY -> "start"
            PrivilegedIntentVerb.START_SERVICE -> "start-service"
        }
        val builder = StringBuilder("am $verb -a \"$action\"")
        if (component != null) builder.append(" -n \"$component\"")
        for ((extraKey, extraValue) in config.extraStringPairs) {
            builder.append(" --es \"$extraKey\" \"$extraValue\"")
        }
        val result = shell.exec(builder.toString())
        val tail = result.stdout.joinToString("\n").take(MAX_TAIL_CHARS)
        return if (result.isSuccess) {
            AutomationControllerResult.IntentResult(exitCode = 0, tail = tail)
        } else {
            AutomationControllerResult.HardwareError("am $verb exited non-zero: $tail")
        }
    }
}
