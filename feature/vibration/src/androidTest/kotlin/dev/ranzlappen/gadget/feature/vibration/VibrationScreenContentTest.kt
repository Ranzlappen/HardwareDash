package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [VibrationScreenContent] — exercised
 * with curated [VibrationScreenState] snapshots, capturing dispatched
 * [VibrationUiEvent]s. Hilt-free (the monitor slots default to no-ops). Mirror
 * of `TorchScreenContentTest`. Runs via
 * `:feature:vibration:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class VibrationScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: VibrationScreenState,
        events: MutableList<VibrationUiEvent> = mutableListOf(),
    ): MutableList<VibrationUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                VibrationScreenContent(
                    state = state,
                    onEvent = { events += it },
                    onResolveIcon = { WidgetIconSource.Resource(R.drawable.ic_vibration_on) },
                )
            }
        }
        return events
    }

    @Test
    fun rendersScreenTitle() {
        setContent(VibrationScreenState.Initial.copy(vibration = VibrationState(isAvailable = true)))
        composeTestRule.onNodeWithText(res.getString(R.string.vibration_screen_title)).assertIsDisplayed()
    }

    @Test
    fun playDispatchesOneShot() {
        val events = setContent(
            VibrationScreenState.Initial.copy(vibration = VibrationState(isAvailable = true)),
        )
        // Both the controls card and the pattern builder expose a "Play"
        // button. The builder's is disabled until a pattern is drawn (none in
        // Initial), so select the enabled one — the one-shot control.
        composeTestRule
            .onAllNodesWithText(res.getString(R.string.vibration_controls_play))
            .filterToOne(isEnabled())
            .performClick()
        assertTrue(events.contains(VibrationUiEvent.OneShot))
    }

    @Test
    fun emptyWidgetsListShowsEmptyState() {
        setContent(VibrationScreenState.Initial)
        composeTestRule
            .onNodeWithText(res.getString(R.string.vibration_widget_list_empty_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun savedWidgetRendersByName() {
        val saved = listOf(
            SavedVibrationWidget(
                appWidgetId = 1,
                config = VibrationWidgetConfig(displayName = "Quick buzz"),
            ),
        )
        setContent(VibrationScreenState.Initial.copy(widgets = saved))
        composeTestRule.onNodeWithContentDescription("Quick buzz").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deleteButtonDispatchesDeleteWidget() {
        val widget = SavedVibrationWidget(
            appWidgetId = 7,
            config = VibrationWidgetConfig(displayName = "Test"),
        )
        val events = setContent(VibrationScreenState.Initial.copy(widgets = listOf(widget)))
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.vibration_widget_list_action_delete))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(VibrationUiEvent.DeleteWidget(widget)))
    }
}
