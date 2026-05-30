package dev.ranzlappen.gadget.feature.torch.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.torch.TorchController
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the action surface [TorchActionHandler] exposes to the
 * future automation engine. The torch-on/off branches are the safe ones to
 * pin here (they reach only the injected [TorchController]); the strobe and
 * morse branches go through `Context.startForegroundService`, which needs a
 * real Android runtime + service to exercise meaningfully and lives behind
 * instrumented tests.
 */
class TorchActionHandlerTest {

    @Test
    fun `ACTION_TORCH_ON sets the controller on`() = runTest {
        val controller = mockk<TorchController>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val handler = TorchActionHandler(context, controller)

        val result = handler.dispatch(TorchActionHandler.ACTION_TORCH_ON, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify { controller.setOn(true) }
    }

    @Test
    fun `ACTION_TORCH_OFF sets the controller off`() = runTest {
        val controller = mockk<TorchController>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val handler = TorchActionHandler(context, controller)

        val result = handler.dispatch(TorchActionHandler.ACTION_TORCH_OFF, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify { controller.setOn(false) }
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val controller = mockk<TorchController>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val handler = TorchActionHandler(context, controller)

        val result = handler.dispatch("not-a-real-action", emptyMap())

        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        val controller = mockk<TorchController>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val handler = TorchActionHandler(context, controller)

        // Pinning the featureId so a future rename surfaces here instead of
        // silently breaking the @StringKey IntoMap binding the automation
        // engine looks the handler up by.
        assertEquals(TorchActionHandler.FEATURE_ID, handler.featureId)
    }
}
