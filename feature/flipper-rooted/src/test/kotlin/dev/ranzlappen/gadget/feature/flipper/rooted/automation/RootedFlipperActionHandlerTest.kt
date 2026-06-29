package dev.ranzlappen.gadget.feature.flipper.rooted.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.flipper.rooted.FlipperRootResult
import dev.ranzlappen.gadget.feature.flipper.rooted.RootedFlipperController
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootedFlipperActionHandlerTest {

    private val controller = mockk<RootedFlipperController>()
    private val handler = RootedFlipperActionHandler(controller)

    @Test
    fun `unknown action is unsupported`() = runTest {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `grant maps Ok to Success`() = runTest {
        coEvery { controller.grantUsbAccess() } returns FlipperRootResult.Ok
        assertEquals(
            ActionResult.Success,
            handler.dispatch(RootedFlipperActionHandler.ACTION_GRANT_USB, emptyMap()),
        )
    }

    @Test
    fun `no device and opted-out map to failures`() = runTest {
        coEvery { controller.grantUsbAccess() } returns FlipperRootResult.NoDevice
        assertTrue(
            handler.dispatch(RootedFlipperActionHandler.ACTION_GRANT_USB, emptyMap())
                is ActionResult.Failure,
        )
        coEvery { controller.grantUsbAccess() } returns FlipperRootResult.OptedOut
        assertTrue(
            handler.dispatch(RootedFlipperActionHandler.ACTION_GRANT_USB, emptyMap())
                is ActionResult.Failure,
        )
    }

    @Test
    fun `unsupported maps to Unsupported`() = runTest {
        coEvery { controller.grantUsbAccess() } returns FlipperRootResult.Unsupported
        assertEquals(
            ActionResult.Unsupported,
            handler.dispatch(RootedFlipperActionHandler.ACTION_GRANT_USB, emptyMap()),
        )
    }

    @Test
    fun `the grant action is declared requiresRoot`() {
        val action = handler.actions.single { it.key == RootedFlipperActionHandler.ACTION_GRANT_USB }
        assertTrue(action.requiresRoot)
    }
}
