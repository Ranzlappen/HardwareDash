package dev.ranzlappen.gadget.core.automation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * One-shot landing point for [AutomationScheduler]'s alarms: fires the rule
 * through the shared [RuleFireExecutor] pipeline, then arms the next
 * occurrence. No resident service is involved — this *is* the schedule
 * rules' "start, evaluate, dispatch, stop" path from the design doc's
 * Runtime host section.
 *
 * **Not** `@AndroidEntryPoint`: the repo's receivers (widget providers,
 * `BootCompletedReceiver`) reach Hilt via [EntryPointAccessors] instead —
 * Hilt's ASM transform for receivers with an `onReceive` override looks for
 * the generated base class in the javac output dir, which a Kotlin-only
 * module doesn't produce (CI: `Hilt_AutomationAlarmReceiver.class (No such
 * file or directory)`).
 */
class AutomationAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlarmEntryPoint {
        fun ruleRepository(): RuleRepository
        fun fireExecutor(): RuleFireExecutor
        fun scheduler(): AutomationScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCHEDULE_FIRED) return
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmEntryPoint::class.java,
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val rule = entry.ruleRepository().rule(ruleId) ?: return@launch
                if (rule.trigger !is Trigger.Schedule) return@launch
                entry.fireExecutor().fire(rule)
                entry.scheduler().scheduleNext(rule)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SCHEDULE_FIRED =
            "dev.ranzlappen.gadget.core.automation.ACTION_SCHEDULE_FIRED"
        const val EXTRA_RULE_ID = "rule_id"
    }
}
