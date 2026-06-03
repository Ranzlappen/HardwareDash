package dev.ranzlappen.gadget.feature.torch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for [TorchScreenContent].
 *
 * Exercises the stateless screen with curated [TorchScreenState]
 * snapshots and asserts the rendered title / status / button state.
 * Captures every dispatched [TorchUiEvent] into a list so callback
 * assertions are a single, type-safe pattern.
 *
 * Tests run via `:feature:torch:connectedDebugAndroidTest`. CI
 * emulator workflow tracked at
 * https://github.com/Ranzlappen/HardwareDash/issues/92.
 */
@RunWith(AndroidJUnit4::class)
class TorchScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: TorchScreenState,
        events: MutableList<TorchUiEvent> = mutableListOf(),
        onResolveIcon: (String) -> WidgetIconSource = {
            WidgetIconSource.Resource(R.drawable.ic_flashlight_on)
        },
    ): MutableList<TorchUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = state,
                    onEvent = { events += it },
                    onResolveIcon = onResolveIcon,
                )
            }
        }
        return events
    }

    @Test
    fun rendersOffStateWhenTorchIsOff() {
        setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = false, isAvailable = true),
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.torch_state_off))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.torch_status_off))
            .assertIsDisplayed()
    }

    @Test
    fun rendersOnStateWhenTorchIsOn() {
        setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = true, isAvailable = true),
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.torch_state_on))
            .assertIsDisplayed()
    }

    @Test
    fun toggleFabIsDisabledWhenUnavailable() {
        setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(
                    isOn = false,
                    isAvailable = false,
                    error = TorchError.NoFlashUnit,
                ),
            ),
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_turn_on))
            .assertIsNotEnabled()
    }

    @Test
    fun toggleClickDispatchesToggleEvent() {
        val events = setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = false, isAvailable = true),
            ),
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_turn_on))
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf(TorchUiEvent.ToggleClick), events)
    }

    @Test
    fun emptyWidgetsListShowsEmptyState() {
        setContent(TorchScreenState.Initial)
        composeTestRule
            .onNodeWithText(res.getString(R.string.torch_widget_list_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun savedWidgetsRender() {
        val saved = listOf(
            SavedTorchWidget(
                appWidgetId = 1,
                config = TorchWidgetConfig(
                    displayName = "Loud strobe",
                    actionKey = TorchWidgetConfig.FUNCTION_STROBE,
                    params = mapOf("rate_hz" to "12.0"),
                ),
            ),
        )
        setContent(TorchScreenState.Initial.copy(widgets = saved))
        // The row shows the live preview (no title text); the widget name
        // is carried as the preview's content description.
        composeTestRule.onNodeWithContentDescription("Loud strobe").assertIsDisplayed()
    }

    @Test
    fun deleteButtonDispatchesDeleteWidget() {
        val widget = SavedTorchWidget(
            appWidgetId = 7,
            config = TorchWidgetConfig(
                displayName = "Test",
                actionKey = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
            ),
        )
        val events = setContent(
            TorchScreenState.Initial.copy(widgets = listOf(widget)),
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_widget_list_action_delete))
            .performClick()
        assertTrue(events.contains(TorchUiEvent.DeleteWidget(widget)))
    }

    @Test
    fun strobeButtonDispatchesStrobeToggle() {
        val events = setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = false, isAvailable = true),
            ),
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_strobe_toggle))
            .performClick()
        assertEquals(listOf(TorchUiEvent.StrobeToggle), events)
    }
}
