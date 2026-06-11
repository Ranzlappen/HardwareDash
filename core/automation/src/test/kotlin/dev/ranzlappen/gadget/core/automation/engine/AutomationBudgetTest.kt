package dev.ranzlappen.gadget.core.automation.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The storm budget is pure + clock-injected, so its whole policy is
 * JVM-testable: per-cycle clamp, rolling-window clamp, window expiry freeing
 * budget, and the throttled flag.
 */
class AutomationBudgetTest {

    @Test
    fun underBothCaps_admitsAll_notThrottled() {
        val budget = AutomationBudget()
        val a = budget.admit(now = 1_000L, requested = 5)
        assertEquals(5, a.allowed)
        assertFalse(a.throttled)
    }

    @Test
    fun perCycleCap_clampsAndThrottles() {
        val budget = AutomationBudget(maxPerCycle = 16, maxPerWindow = 1_000)
        val a = budget.admit(now = 0L, requested = 20)
        assertEquals(16, a.allowed)
        assertTrue(a.throttled)
    }

    @Test
    fun rollingWindowCap_clampsAcrossCycles() {
        val budget = AutomationBudget(maxPerCycle = 100, maxPerWindow = 60, windowMs = 60_000L)
        // 60 admitted across two cycles fills the window.
        assertEquals(40, budget.admit(now = 0L, requested = 40).allowed)
        assertEquals(20, budget.admit(now = 1_000L, requested = 30).allowed) // 40+20=60
        // Window full → next cycle admits nothing and reports throttled.
        val a = budget.admit(now = 2_000L, requested = 5)
        assertEquals(0, a.allowed)
        assertTrue(a.throttled)
    }

    @Test
    fun windowExpiry_freesBudget() {
        val budget = AutomationBudget(maxPerCycle = 100, maxPerWindow = 60, windowMs = 60_000L)
        assertEquals(60, budget.admit(now = 0L, requested = 60).allowed)
        // 1 ms before the window elapses the t=0 batch is still live → full.
        assertEquals(0, budget.admit(now = 59_999L, requested = 10).allowed)
        // At exactly windowMs the t=0 batch ages out (cutoff `<= now-windowMs`),
        // freeing the whole window.
        val a = budget.admit(now = 60_000L, requested = 10)
        assertEquals(10, a.allowed)
        assertFalse(a.throttled)
    }

    @Test
    fun partialWindowExpiry_freesProportionally() {
        val budget = AutomationBudget(maxPerCycle = 100, maxPerWindow = 60, windowMs = 60_000L)
        budget.admit(now = 0L, requested = 30)       // 30 at t=0
        budget.admit(now = 30_000L, requested = 30)  // 30 at t=30s → window full
        // At t=60_001 the t=0 batch (30) has aged out; 30 from t=30s remain.
        val a = budget.admit(now = 60_001L, requested = 40)
        assertEquals(30, a.allowed) // 60 - 30 still-live = 30 free
        assertTrue(a.throttled)
    }

    @Test
    fun zeroRequested_isNoOp() {
        val budget = AutomationBudget()
        val a = budget.admit(now = 0L, requested = 0)
        assertEquals(0, a.allowed)
        assertFalse(a.throttled)
        // Didn't consume window budget.
        assertEquals(16, budget.admit(now = 0L, requested = 100).allowed)
    }

    @Test
    fun defaults_matchTheSpec() {
        assertEquals(16, AutomationBudget.DEFAULT_MAX_PER_CYCLE)
        assertEquals(60, AutomationBudget.DEFAULT_MAX_PER_WINDOW)
        assertEquals(60_000L, AutomationBudget.DEFAULT_WINDOW_MS)
    }
}
