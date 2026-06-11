package dev.ranzlappen.gadget.core.automation.engine

/**
 * The pure exact/inexact degradation **decision** for a
 * `Trigger.Schedule`'s alarm (ADR-0002 Decision 4; the contract table in
 * `docs/automation-engine.md` § Runtime host). Separated from the
 * `AutomationScheduler` that calls `AlarmManager` so the whole policy is
 * JVM-unit-testable with no Android — the same pure-function discipline as
 * the rest of the engine.
 *
 * The three states, by `(exactRequested, canScheduleExactAlarms)`:
 *
 * | exactRequested | canScheduleExactAlarms | plan |
 * |---|---|---|
 * | false (default) | — | [AlarmExactness.WindowedInexact], no badge |
 * | true | true | [AlarmExactness.ExactAllowWhileIdle], no badge |
 * | true | false (denied) | [AlarmExactness.WindowedInexact] + **needs-permission badge** |
 *
 * `SCHEDULE_EXACT_ALARM` is denied by default on Android 14+ fresh installs
 * (SDK 33+), so the default path must be inexact and the `exact` opt-in must
 * tolerate denial. `USE_EXACT_ALARM` is deliberately not used (Play restricts
 * it to alarm-clock/calendar apps).
 */
object AlarmSchedulingDecision {

    fun plan(exactRequested: Boolean, canScheduleExactAlarms: Boolean): ScheduleAlarmPlan = when {
        !exactRequested ->
            ScheduleAlarmPlan(AlarmExactness.WindowedInexact, needsExactAlarmPermission = false)
        canScheduleExactAlarms ->
            ScheduleAlarmPlan(AlarmExactness.ExactAllowWhileIdle, needsExactAlarmPermission = false)
        else ->
            // exact requested but the special permission is denied: fall back
            // to a window and surface the "needs Alarms & reminders" badge.
            ScheduleAlarmPlan(AlarmExactness.WindowedInexact, needsExactAlarmPermission = true)
    }
}

/** Which `AlarmManager` call the scheduler should make. */
enum class AlarmExactness {
    /** `setExactAndAllowWhileIdle` — fires at the minute, even in Doze. */
    ExactAllowWhileIdle,

    /** `setWindow` (±10 min) — the battery-friendly, always-available default. */
    WindowedInexact,
}

/**
 * The scheduler's chosen [exactness] plus whether the builder UI should show
 * the "needs Alarms & reminders" badge linking
 * `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (true only when the user asked for
 * exact but the permission is denied).
 */
data class ScheduleAlarmPlan(
    val exactness: AlarmExactness,
    val needsExactAlarmPermission: Boolean,
)
