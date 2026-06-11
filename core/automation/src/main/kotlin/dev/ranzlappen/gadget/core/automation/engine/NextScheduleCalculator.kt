package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.DayOfWeek
import dev.ranzlappen.gadget.core.automation.model.Trigger
import java.time.Instant
import java.time.ZoneId

/**
 * Pure next-occurrence math for [Trigger.Schedule] — when should the next
 * alarm fire, given "minutes after local midnight on these weekdays"?
 * Separated from [dev.ranzlappen.gadget.core.automation.service.AutomationScheduler]
 * (which owns the `AlarmManager` calls) so the calendar logic is JVM-tested,
 * same discipline as [AlarmSchedulingDecision].
 *
 * Semantics:
 *  - The next fire is the earliest `date.atStartOfDay(zone) +
 *    timeOfDayMinutes` that is **strictly after** `nowMillis`, scanning
 *    today plus the next 7 days (a non-empty day set always matches within
 *    that span).
 *  - Building from `atStartOfDay + minutes` (not `atTime`) keeps DST shift
 *    days sane: the wall-clock target slides with the offset change instead
 *    of throwing on a non-existent local time.
 *  - An **empty** [Trigger.Schedule.daysOfWeek] yields `null` (nothing to
 *    schedule); the default is every day, so this only arises from a
 *    hand-built rule.
 */
object NextScheduleCalculator {

    fun nextFireAtMillis(schedule: Trigger.Schedule, nowMillis: Long, zone: ZoneId): Long? {
        if (schedule.daysOfWeek.isEmpty()) return null
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        for (dayOffset in 0L..7L) {
            val date = today.plusDays(dayOffset)
            if (date.dayOfWeek.toModel() !in schedule.daysOfWeek) continue
            val fireAt = date.atStartOfDay(zone)
                .plusMinutes(schedule.timeOfDayMinutes.toLong())
                .toInstant()
                .toEpochMilli()
            if (fireAt > nowMillis) return fireAt
        }
        return null // unreachable for a non-empty day set; defensive.
    }

    private fun java.time.DayOfWeek.toModel(): DayOfWeek = when (this) {
        java.time.DayOfWeek.MONDAY -> DayOfWeek.Monday
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.Tuesday
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.Wednesday
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.Thursday
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.Friday
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.Saturday
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.Sunday
    }
}
