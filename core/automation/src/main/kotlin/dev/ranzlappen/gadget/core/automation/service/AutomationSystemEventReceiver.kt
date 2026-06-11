package dev.ranzlappen.gadget.core.automation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.SystemEventKind
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * One-shot landing point for [Trigger.SystemEvent] rules. Manifest-registered
 * for the power broadcasts ([Intent.ACTION_POWER_CONNECTED] /
 * [Intent.ACTION_POWER_DISCONNECTED]), which are on the implicit-broadcast
 * exemption list and therefore still delivered to manifest receivers.
 *
 * **Scope notes** (mirrored in `docs/automation-engine.md`):
 *  - [SystemEventKind.Connectivity] is **not armed here** — connectivity
 *    broadcasts stopped being deliverable to manifest receivers in Android N;
 *    arming it needs a registered `NetworkCallback` inside a resident
 *    component, queued behind the builder UI batch.
 *  - [SystemEventKind.BootCompleted] rules fire from the widgetkit
 *    `BootCompletedReceiver` path (the `:app` automation boot-rearm handler),
 *    not here — one boot receiver for the whole app, per the kit's rule.
 *
 * **Not** `@AndroidEntryPoint` — see [AutomationAlarmReceiver]'s note (the
 * repo's receivers use [EntryPointAccessors]; Hilt's receiver ASM transform
 * breaks on Kotlin-only modules).
 */
class AutomationSystemEventReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SystemEventEntryPoint {
        fun ruleRepository(): RuleRepository
        fun fireExecutor(): RuleFireExecutor
    }

    override fun onReceive(context: Context, intent: Intent) {
        val kind = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> SystemEventKind.PowerConnected
            Intent.ACTION_POWER_DISCONNECTED -> SystemEventKind.PowerDisconnected
            else -> return
        }
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SystemEventEntryPoint::class.java,
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                entry.ruleRepository().observeRules().first()
                    .filter { it.enabled && it.trigger == Trigger.SystemEvent(kind) }
                    .forEach { entry.fireExecutor().fire(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
