package dev.ranzlappen.gadget.core.automation.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One test per cell of the design doc's three-state degradation table. */
class AlarmSchedulingDecisionTest {

    @Test
    fun inexactByDefault_regardlessOfPermission() {
        for (canExact in listOf(true, false)) {
            val plan = AlarmSchedulingDecision.plan(exactRequested = false, canScheduleExactAlarms = canExact)
            assertEquals(AlarmExactness.WindowedInexact, plan.exactness)
            assertFalse("default path never asks for the permission", plan.needsExactAlarmPermission)
        }
    }

    @Test
    fun exactRequested_andGranted_usesExact() {
        val plan = AlarmSchedulingDecision.plan(exactRequested = true, canScheduleExactAlarms = true)
        assertEquals(AlarmExactness.ExactAllowWhileIdle, plan.exactness)
        assertFalse(plan.needsExactAlarmPermission)
    }

    @Test
    fun exactRequested_butDenied_fallsBackToWindowed_withBadge() {
        val plan = AlarmSchedulingDecision.plan(exactRequested = true, canScheduleExactAlarms = false)
        assertEquals(AlarmExactness.WindowedInexact, plan.exactness)
        assertTrue("denied exact must raise the needs-permission badge", plan.needsExactAlarmPermission)
    }
}
