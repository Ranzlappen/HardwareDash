package dev.ranzlappen.gadget.feature.notification.automation

import android.app.NotificationManager
import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import dev.ranzlappen.gadget.feature.notification.control.StickyOverrideConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.match
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the action surface [NotificationActionHandler] exposes to
 * the automation engine. Mirrors `TorchActionHandlerTest` / `GpsActionHandlerTest`'s
 * shape: the [NotificationController]-backed branches are pinned here (they
 * reach only the injected mock); [NotificationActionHandler.dispatch]'s
 * `post_test_notification` / `cancel_test_notification` branches call the real
 * `NotificationManager`, which needs a real Android runtime to exercise
 * meaningfully and is left to the instrumented screen test.
 */
class NotificationActionHandlerTest {

    private fun newHandler(controller: NotificationController): NotificationActionHandler {
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
            every { getString(any()) } returns ""
        }
        val channelRegistry = mockk<NotificationChannelRegistry>(relaxed = true)
        return NotificationActionHandler(context, controller, channelRegistry)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        val handler = newHandler(mockk<NotificationController>(relaxed = true))
        assertEquals(NotificationActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val handler = newHandler(mockk<NotificationController>(relaxed = true))
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `sticky override dispatches to the controller with the given channel id`() = runTest {
        val controller = mockk<NotificationController>()
        coEvery { controller.overrideStickyChannel(any()) } returns
            NotificationControllerResult.ChannelImportanceSnapshot("alerts", 2, 4)
        val handler = newHandler(controller)

        val result = handler.dispatch(
            NotificationActionHandler.ACTION_STICKY_OVERRIDE,
            mapOf(NotificationActionHandler.PARAM_CHANNEL_ID to "alerts"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.overrideStickyChannel(StickyOverrideConfig("alerts")) }
    }

    @Test
    fun `sticky override without a channel id fails without calling the controller`() = runTest {
        val controller = mockk<NotificationController>()
        val handler = newHandler(controller)

        val result = handler.dispatch(NotificationActionHandler.ACTION_STICKY_OVERRIDE, emptyMap())

        assertTrue(result is ActionResult.Failure)
        coVerify(exactly = 0) { controller.overrideStickyChannel(any()) }
    }

    @Test
    fun `grant listener maps controller Unsupported to a Failure`() = runTest {
        val controller = mockk<NotificationController>()
        coEvery { controller.grantListenerAccess() } returns NotificationControllerResult.Unsupported
        val handler = newHandler(controller)

        val result = handler.dispatch(NotificationActionHandler.ACTION_GRANT_LISTENER, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `grant listener maps controller OptedOut to a Failure`() = runTest {
        val controller = mockk<NotificationController>()
        coEvery { controller.grantListenerAccess() } returns NotificationControllerResult.OptedOut
        val handler = newHandler(controller)

        val result = handler.dispatch(NotificationActionHandler.ACTION_GRANT_LISTENER, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `show overlay dispatches with parsed message and duration`() = runTest {
        val controller = mockk<NotificationController>()
        coEvery { controller.showLockScreenOverlay(any()) } returns NotificationControllerResult.Ok()
        val handler = newHandler(controller)

        val result = handler.dispatch(
            NotificationActionHandler.ACTION_SHOW_OVERLAY,
            mapOf(
                NotificationActionHandler.PARAM_MESSAGE to "hello",
                NotificationActionHandler.PARAM_DURATION_MS to "9000",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            controller.showLockScreenOverlay(
                match { it.message == "hello" && it.durationMillis == 9000L },
            )
        }
    }

    @Test
    fun `reset all maps ResetCompleted to Success`() = runTest {
        val controller = mockk<NotificationController>()
        coEvery { controller.resetAllNotificationMutations() } returns
            NotificationControllerResult.ResetCompleted(restored = 2, failed = 0)
        val handler = newHandler(controller)

        val result = handler.dispatch(NotificationActionHandler.ACTION_RESET_ALL, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert channel importance fails when the channel does not exist`() = runTest {
        val notificationManager = mockk<NotificationManager>(relaxed = true) {
            every { getNotificationChannel("missing") } returns null
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        }
        val handler = NotificationActionHandler(
            context,
            mockk<NotificationController>(relaxed = true),
            mockk<NotificationChannelRegistry>(relaxed = true),
        )

        val result = handler.dispatch(
            NotificationActionHandler.ACTION_ASSERT_CHANNEL_IMPORTANCE,
            mapOf(NotificationActionHandler.PARAM_CHANNEL_ID to "missing"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `assert channel importance without a channel id fails`() = runTest {
        val handler = newHandler(mockk<NotificationController>(relaxed = true))

        val result = handler.dispatch(NotificationActionHandler.ACTION_ASSERT_CHANNEL_IMPORTANCE, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }
}
