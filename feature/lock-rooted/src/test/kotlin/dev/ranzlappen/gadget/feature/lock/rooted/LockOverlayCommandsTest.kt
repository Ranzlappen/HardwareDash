package dev.ranzlappen.gadget.feature.lock.rooted

import org.junit.Assert.assertEquals
import org.junit.Test

class LockOverlayCommandsTest {

    @Test
    fun `grant command targets the given package and SYSTEM_ALERT_WINDOW`() {
        assertEquals(
            "appops set dev.ranzlappen.gadget.rooted SYSTEM_ALERT_WINDOW allow",
            LockOverlayCommands.grantOverlayPermission("dev.ranzlappen.gadget.rooted"),
        )
    }

    @Test
    fun `clampDuration raises sub-minimum requests to the floor`() {
        assertEquals(
            LockOverlayCommands.MIN_DURATION_MILLIS,
            LockOverlayCommands.clampDuration(0),
        )
    }

    @Test
    fun `clampDuration caps over-ceiling requests`() {
        assertEquals(
            LockOverlayCommands.HARD_CEILING_MILLIS,
            LockOverlayCommands.clampDuration(Long.MAX_VALUE),
        )
    }

    @Test
    fun `clampDuration passes an in-range request through`() {
        assertEquals(5_000L, LockOverlayCommands.clampDuration(5_000L))
    }
}
