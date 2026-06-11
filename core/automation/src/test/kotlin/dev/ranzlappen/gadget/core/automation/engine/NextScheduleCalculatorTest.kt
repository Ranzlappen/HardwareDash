package dev.ranzlappen.gadget.core.automation.engine

import dev.ranzlappen.gadget.core.automation.model.DayOfWeek
import dev.ranzlappen.gadget.core.automation.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calendar math for the schedule alarms, against a fixed zone so the tests
 * are machine-independent. 2026-06-10 is a Wednesday.
 */
class NextScheduleCalculatorTest {

    private val zone = ZoneId.of("Europe/Berlin")

    /** Wed 2026-06-10 12:00 in [zone]. */
    private val wednesdayNoon =
        ZonedDateTime.of(2026, 6, 10, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun millisOf(y: Int, m: Int, d: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(y, m, d, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun schedule(minutes: Int, days: Set<DayOfWeek> = DayOfWeek.everyDay()) =
        Trigger.Schedule(timeOfDayMinutes = minutes, daysOfWeek = days)

    @Test
    fun laterToday_firesToday() {
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 14 * 60), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 10, 14, 0), next)
    }

    @Test
    fun earlierToday_rollsToTomorrow() {
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 9 * 60), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 11, 9, 0), next)
    }

    @Test
    fun exactlyNow_rollsForward_strictlyAfter() {
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 12 * 60), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 11, 12, 0), next)
    }

    @Test
    fun daySetFiltering_skipsToTheNextAllowedDay() {
        // Only Saturdays; from Wednesday noon → Saturday 2026-06-13 09:00.
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 9 * 60, days = setOf(DayOfWeek.Saturday)), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 13, 9, 0), next)
    }

    @Test
    fun sameDayOnly_earlierTime_rollsAFullWeek() {
        // Only Wednesdays, 09:00, asked at Wednesday noon → next Wednesday.
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 9 * 60, days = setOf(DayOfWeek.Wednesday)), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 17, 9, 0), next)
    }

    @Test
    fun midnightSchedule_firesAtStartOfNextDay() {
        val next = NextScheduleCalculator.nextFireAtMillis(
            schedule(minutes = 0), wednesdayNoon, zone,
        )
        assertEquals(millisOf(2026, 6, 11, 0, 0), next)
    }

    @Test
    fun emptyDaySet_yieldsNull() {
        assertNull(
            NextScheduleCalculator.nextFireAtMillis(
                schedule(minutes = 9 * 60, days = emptySet()), wednesdayNoon, zone,
            ),
        )
    }
}
