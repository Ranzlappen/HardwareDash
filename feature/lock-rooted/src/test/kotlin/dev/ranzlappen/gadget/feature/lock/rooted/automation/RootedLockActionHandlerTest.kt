package dev.ranzlappen.gadget.feature.lock.rooted.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.lock.rooted.LockOverlayResult
import dev.ranzlappen.gadget.feature.lock.rooted.RootedLockOverlayController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootedLockActionHandlerTest {

    private val overlay = mockk<RootedLockOverlayController>()
    private val handler = RootedLockActionHandler(overlay)

    @Test
    fun `unknown action is unsupported`() = runTest {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `show overlay maps Ok to Success`() = runTest {
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.Ok
        assertEquals(
            ActionResult.Success,
            handler.dispatch(RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY, emptyMap()),
        )
    }

    @Test
    fun `missing params fall back to default message and duration`() = runTest {
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.Ok
        handler.dispatch(RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY, emptyMap())
        coVerify { overlay.showSecureOverlay(RootedLockActionHandler.DEFAULT_MESSAGE, 3_000L) }
    }

    @Test
    fun `supplied params are forwarded`() = runTest {
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.Ok
        handler.dispatch(
            RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY,
            mapOf(
                RootedLockActionHandler.PARAM_MESSAGE to "Stay out",
                RootedLockActionHandler.PARAM_DURATION_MILLIS to "9000",
            ),
        )
        coVerify { overlay.showSecureOverlay("Stay out", 9_000L) }
    }

    @Test
    fun `opted-out and rate-limited map to failures`() = runTest {
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.OptedOut
        assertTrue(
            handler.dispatch(RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY, emptyMap())
                is ActionResult.Failure,
        )
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.RateLimited(1_500L)
        assertTrue(
            handler.dispatch(RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY, emptyMap())
                is ActionResult.Failure,
        )
    }

    @Test
    fun `unsupported result maps to Unsupported`() = runTest {
        coEvery { overlay.showSecureOverlay(any(), any()) } returns LockOverlayResult.Unsupported
        assertEquals(
            ActionResult.Unsupported,
            handler.dispatch(RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY, emptyMap()),
        )
    }

    @Test
    fun `the show action is declared requiresRoot with both params`() {
        val action = handler.actions.single { it.key == RootedLockActionHandler.ACTION_SHOW_SECURE_OVERLAY }
        assertTrue(action.requiresRoot)
        val paramNames = action.params.map { it.name }
        assertTrue(RootedLockActionHandler.PARAM_MESSAGE in paramNames)
        assertTrue(RootedLockActionHandler.PARAM_DURATION_MILLIS in paramNames)
    }
}
