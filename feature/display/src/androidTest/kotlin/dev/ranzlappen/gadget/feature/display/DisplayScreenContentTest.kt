package dev.ranzlappen.gadget.feature.display

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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [DisplayScreenContent] — mirrors
 * `TorchScreenContentTest` / `SensorsScreenContentTest`. Hilt-free: the
 * `monitors` slot defaults to a no-op, so no ViewModel/DI is exercised.
 */
@RunWith(AndroidJUnit4::class)
class DisplayScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: DisplayState,
        events: MutableList<DisplayUiEvent> = mutableListOf(),
    ): MutableList<DisplayUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                DisplayScreenContent(state = state, moduleInfo = null, onEvent = { events += it })
            }
        }
        return events
    }

    @Test
    fun rendersScreenTitleAndReadouts() {
        setContent(
            DisplayState(
                refreshRateHz = 90f,
                rotationDegrees = 90,
                resolutionWidth = 1080,
                resolutionHeight = 2400,
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.display_screen_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.display_value_hz, 90)).assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.display_value_degrees, 90)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.display_value_resolution, 1080, 2400))
            .assertIsDisplayed()
    }

    @Test
    fun brightnessSlider_disabledWhenWriteSettingsNotGranted() {
        setContent(DisplayState(brightnessPercent = 40, brightnessWritable = false))
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.display_brightness_label), substring = true)
            .assertIsNotEnabled()
    }

    @Test
    fun brightnessSlider_enabledWhenWriteSettingsGranted() {
        setContent(DisplayState(brightnessPercent = 40, brightnessWritable = true))
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.display_brightness_label), substring = true)
            .assertIsEnabled()
    }

    @Test
    fun densitySlider_disabledOnStandardFlavor() {
        setContent(DisplayState(isRootedFlavor = false))
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.display_density_label), substring = true)
            .assertIsNotEnabled()
    }

    @Test
    fun densitySlider_enabledOnRootedFlavor() {
        setContent(DisplayState(isRootedFlavor = true))
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.display_density_label), substring = true)
            .assertIsEnabled()
    }

    @Test
    fun resetAction_firesResetAllRequestedEvent() {
        val events = setContent(DisplayState())
        composeTestRule.onNodeWithText(res.getString(R.string.display_reset_action)).performClick()
        assertEquals(listOf(DisplayUiEvent.ResetAllRequested), events)
    }

    @Test
    fun statusMessage_rendersWhenPresent() {
        setContent(DisplayState(statusMessage = "Density set to 440 dpi"))
        composeTestRule.onNodeWithText("Density set to 440 dpi").assertIsDisplayed()
    }
}
