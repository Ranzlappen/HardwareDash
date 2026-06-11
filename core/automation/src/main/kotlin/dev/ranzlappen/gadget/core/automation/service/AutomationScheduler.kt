package dev.ranzlappen.gadget.core.automation.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.engine.AlarmExactness
import dev.ranzlappen.gadget.core.automation.engine.AlarmSchedulingDecision
import dev.ranzlappen.gadget.core.automation.engine.NextScheduleCalculator
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms one `AlarmManager` alarm per enabled [Trigger.Schedule] rule
 * (ADR-0002 Decision 4: AlarmManager, **not** WorkManager — its 15-minute
 * floor is too coarse for "at 09:00 do X").
 *
 * Pure logic is delegated: the **when** to [NextScheduleCalculator], the
 * **how exactly** to [AlarmSchedulingDecision] (the design doc's three-state
 * degradation contract — inexact ±10 min window by default; per-rule `exact`
 * opt-in behind `SCHEDULE_EXACT_ALARM`, falling back to the window when the
 * permission is denied). This class owns only the Android edges:
 * `canScheduleExactAlarms()` (API 31+; exact alarms below S need no special
 * permission) and the `PendingIntent` plumbing into
 * [AutomationAlarmReceiver].
 *
 * Alarms are **one-shot**: the receiver fires the rule, then calls
 * [scheduleNext] for the following occurrence — repeating alarms can't
 * follow a weekday set. [rearmAll] restores every schedule after boot
 * (alarms don't survive reboot) and after rule-set changes.
 */
@Singleton
class AutomationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
) {
    private val alarmManager: AlarmManager?
        get() = ContextCompat.getSystemService(context, AlarmManager::class.java)

    /** (Re)arm the alarm for [rule]'s next occurrence; no-op for non-Schedule rules. */
    fun scheduleNext(rule: Rule) {
        val trigger = rule.trigger as? Trigger.Schedule ?: return
        if (!rule.enabled) {
            cancel(rule.id)
            return
        }
        val manager = alarmManager ?: return
        val fireAt = NextScheduleCalculator.nextFireAtMillis(
            schedule = trigger,
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
        ) ?: return
        val pi = pendingIntent(rule.id)
        val plan = AlarmSchedulingDecision.plan(
            exactRequested = trigger.exact,
            canScheduleExactAlarms = canScheduleExactAlarms(manager),
        )
        when (plan.exactness) {
            AlarmExactness.ExactAllowWhileIdle ->
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            AlarmExactness.WindowedInexact ->
                manager.setWindow(AlarmManager.RTC_WAKEUP, fireAt, INEXACT_WINDOW_MS, pi)
        }
        // plan.needsExactAlarmPermission is surfaced by the builder UI
        // (batch 3.4) — the scheduler just degrades silently here.
    }

    fun cancel(ruleId: String) {
        alarmManager?.cancel(pendingIntent(ruleId))
    }

    /**
     * Re-arm every enabled Schedule rule — the boot-rearm path (alarms don't
     * survive reboot) and the coarse "rule set changed" path.
     */
    suspend fun rearmAll() {
        ruleRepository.observeRules().first().forEach(::scheduleNext)
    }

    private fun canScheduleExactAlarms(manager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    private fun pendingIntent(ruleId: String): PendingIntent {
        val intent = Intent(context, AutomationAlarmReceiver::class.java)
            .setAction(AutomationAlarmReceiver.ACTION_SCHEDULE_FIRED)
            .putExtra(AutomationAlarmReceiver.EXTRA_RULE_ID, ruleId)
        return PendingIntent.getBroadcast(
            context,
            // One alarm slot per rule: same rule id -> same PendingIntent ->
            // setWindow/setExact replaces rather than stacks.
            ruleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        /** The design-doc contract's ±10 min: a 10-minute delivery window. */
        const val INEXACT_WINDOW_MS = 10L * 60L * 1000L
    }
}
