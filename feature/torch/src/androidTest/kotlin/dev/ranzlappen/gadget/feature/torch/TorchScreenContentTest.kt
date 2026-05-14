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

    @Test
    fun rendersOffStateWhenTorchIsOff() {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(
                        torch = TorchState(isOn = false, isAvailable = true),
                    ),
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

        composeTestRule.onNodeWithText(res.getString(R.string.torch_state_off))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.torch_status_off))
            .assertIsDisplayed()
    }

    @Test
    fun rendersOnStateWhenTorchIsOn() {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(
                        torch = TorchState(isOn = true, isAvailable = true),
                    ),
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

        composeTestRule.onNodeWithText(res.getString(R.string.torch_state_on))
            .assertIsDisplayed()
    }

    @Test
    fun toggleFabIsDisabledWhenUnavailable() {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(
                        torch = TorchState(
                            isOn = false,
                            isAvailable = false,
                            error = TorchError.NoFlashUnit,
                        ),
                    ),
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_turn_on))
            .assertIsNotEnabled()
    }

    @Test
    fun toggleClickInvokesCallback() {
        var clicks = 0
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(
                        torch = TorchState(isOn = false, isAvailable = true),
                    ),
                    onToggleClick = { clicks += 1 },
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_action_turn_on))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun emptyWidgetsListShowsEmptyState() {
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial,
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

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
                    sosMode = false,
                ),
            ),
        )
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(widgets = saved),
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Loud strobe").assertIsDisplayed()
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
        composeTestRule.setContent {
            GadgetTestTheme {
                TorchScreenContent(
                    state = TorchScreenState.Initial.copy(widgets = listOf(widget)),
                    onToggleClick = {},
                    onRateChange = {},
                    onAddFlashlight = {},
                    onAddStrobe = {},
                    onEditWidget = {},
                    onDeleteWidget = { deleted = it },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.torch_widget_list_action_delete))
            .performClick()

        assertTrue(deleted == widget)
    }
}
