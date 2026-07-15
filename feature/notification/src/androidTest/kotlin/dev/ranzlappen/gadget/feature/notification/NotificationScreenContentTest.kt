package dev.ranzlappen.gadget.feature.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [NotificationScreenContent] —
 * exercised with curated [NotificationScreenState] snapshots, capturing
 * dispatched [NotificationUiEvent]s. Hilt-free (the monitor slots default to
 * no-ops). Mirrors `TorchScreenContentTest` / `VibrationScreenContentTest`.
 * Runs via `:feature:notification:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class NotificationScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: NotificationScreenState,
        events: MutableList<NotificationUiEvent> = mutableListOf(),
    ): MutableList<NotificationUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                NotificationScreenContent(
                    state = state,
                    onEvent = { events += it },
                )
            }
        }
        return events
    }

    @Test
    fun rendersBuilderCardOnStandardFlavor() {
        setContent(NotificationScreenState.Initial.copy(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_builder_card_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_channel_inspector_card_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun rootedOnlyCardsAreHiddenOnStandardFlavor() {
        setContent(NotificationScreenState.Initial.copy(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_sticky_override_card_title))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_overlay_card_title))
            .assertDoesNotExist()
    }

    @Test
    fun rootedOnlyCardsRenderOnRootedFlavor() {
        setContent(NotificationScreenState.Initial.copy(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_sticky_override_card_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_listener_access_card_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_overlay_card_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_reset_card_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun postButtonIsAlwaysEnabledAndDispatchesEvent() {
        val events = setContent(NotificationScreenState.Initial)
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_builder_post))
            .assertIsEnabled()
            .performClick()
        assertTrue(events.contains(NotificationUiEvent.PostTestNotification))
    }

    @Test
    fun cancelButtonIsDisabledUntilANotificationHasBeenPosted() {
        setContent(NotificationScreenState.Initial.copy(lastPostedNotificationId = null))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_builder_cancel))
            .assertIsNotEnabled()
    }

    @Test
    fun cancelButtonIsEnabledAfterAPost() {
        val events = setContent(NotificationScreenState.Initial.copy(lastPostedNotificationId = 42))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_builder_cancel))
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf(NotificationUiEvent.CancelTestNotification), events)
    }

    @Test
    fun typingATitleDispatchesBuilderTitleChange() {
        val events = setContent(NotificationScreenState.Initial)
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_builder_title_label))
            .performTextInput("Hi")
        assertTrue(events.any { it is NotificationUiEvent.BuilderTitleChange })
    }

    @Test
    fun emptyChannelListShowsEmptyState() {
        setContent(NotificationScreenState.Initial.copy(channels = emptyList()))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_channel_inspector_empty))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun channelsRenderInTheInspector() {
        setContent(
            NotificationScreenState.Initial.copy(
                channels = listOf(NotificationChannelSummary("alerts", "Alerts", 4)),
            ),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_channel_importance_row, "Alerts", "High"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun stickyOverrideButtonDispatchesEventWithChannelId() {
        val events = setContent(
            NotificationScreenState.Initial.copy(isRootedFlavor = true, stickyChannelId = "alerts"),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_sticky_override_button))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(NotificationUiEvent.StickyOverrideRequest))
    }

    @Test
    fun resetAllButtonDispatchesEvent() {
        val events = setContent(NotificationScreenState.Initial.copy(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.notification_reset_button))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(NotificationUiEvent.ResetAllRequest))
    }
}
