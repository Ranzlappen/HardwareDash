package dev.ranzlappen.gadget.core.automation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot landing point for [AutomationScheduler]'s alarms: fires the rule
 * through the shared [RuleFireExecutor] pipeline, then arms the next
 * occurrence. No resident service is involved — this *is* the schedule
 * rules' "start, evaluate, dispatch, stop" path from the design doc's
 * Runtime host section.
 */
@AndroidEntryPoint
class AutomationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var fireExecutor: RuleFireExecutor
    @Inject lateinit var scheduler: AutomationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCHEDULE_FIRED) return
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val rule = ruleRepository.rule(ruleId) ?: return@launch
                if (rule.trigger !is Trigger.Schedule) return@launch
                fireExecutor.fire(rule)
                scheduler.scheduleNext(rule)
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
