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
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for [TorchScreenContent].
 *
 * Exercises the stateless screen with curated [TorchScreenState]
 * snapshots and asserts that the rendered title / status / button
 * state matches.
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
        onToggleClick: () -> Unit = {},
        onMomentaryHold: (Boolean) -> Unit = {},
        onStrobeToggle: () -> Unit = {},
        onStrobeHold: (Boolean) -> Unit = {},
        onMorseToggle: () -> Unit = {},
        onMorseHold: (Boolean) -> Unit = {},
        onMorseTextChange: (String) -> Unit = {},
        onRateChange: (Float) -> Unit = {},
        onRateCommit: () -> Unit = {},
        onAddFlashlight: () -> Unit = {},
        onAddStrobe: () -> Unit = {},
        onEditWidget: (SavedTorchWidget) -> Unit = {},
        onDeleteWidget: (SavedTorchWidget) -> Unit = {},
        onResolveIcon: (String) -> WidgetIconSource = {
            WidgetIconSource.Resource(R.drawable.ic_flashlight_on)
        },
    ) {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = state,
                    onToggleClick = onToggleClick,
                    onMomentaryHold = onMomentaryHold,
                    onStrobeToggle = onStrobeToggle,
                    onStrobeHold = onStrobeHold,
                    onMorseToggle = onMorseToggle,
                    onMorseHold = onMorseHold,
                    onMorseTextChange = onMorseTextChange,
                    onRateChange = onRateChange,
                    onRateCommit = onRateCommit,
                    onAddFlashlight = onAddFlashlight,
                    onAddStrobe = onAddStrobe,
                    onEditWidget = onEditWidget,
                    onDeleteWidget = onDeleteWidget,
                    onResolveIcon = onResolveIcon,
                )
            }
        }
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
    fun toggleClickInvokesCallback() {
        var clicks = 0
        setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = false, isAvailable = true),
            ),
            onToggleClick = { clicks += 1 },
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_turn_on))
            .assertIsEnabled()
            .performClick()
        assertEquals(1, clicks)
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
                    type = WidgetType.Strobe,
                    displayName = "Loud strobe",
                    rateHz = 12f,
                    morseMode = false,
                ),
            ),
        )
        setContent(TorchScreenState.Initial.copy(widgets = saved))
        // The row shows the live preview (no title text); the widget name
        // is carried as the preview's content description.
        composeTestRule.onNodeWithContentDescription("Loud strobe").assertIsDisplayed()
    }

    @Test
    fun deleteButtonInvokesCallback() {
        val widget = SavedTorchWidget(
            appWidgetId = 7,
            config = TorchWidgetConfig(
                type = WidgetType.Flashlight,
                displayName = "Test",
            ),
        )
        var deleted: SavedTorchWidget? = null
        setContent(
            TorchScreenState.Initial.copy(widgets = listOf(widget)),
            onDeleteWidget = { deleted = it },
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_widget_list_action_delete))
            .performClick()
        assertTrue(deleted == widget)
    }

    @Test
    fun strobeButtonStartsAndStopsViaCallback() {
        var toggles = 0
        setContent(
            TorchScreenState.Initial.copy(
                torch = TorchState(isOn = false, isAvailable = true),
            ),
            onStrobeToggle = { toggles += 1 },
        )
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_strobe_toggle))
            .performClick()
        assertEquals(1, toggles)
    }
}
