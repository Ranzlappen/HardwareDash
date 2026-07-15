package dev.ranzlappen.gadget.feature.battery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [BatteryScreenContent].
 *
 * Exercises the stateless screen with a representative [BatteryState]
 * snapshot and asserts the rendered screen title + card headers. Mirrors
 * the torch reference pattern (`createComposeRule` + [GadgetTestTheme]).
 *
 * Tests run via `:feature:battery:connectedDebugAndroidTest`. CI emulator
 * workflow tracked at https://github.com/Ranzlappen/HardwareDash/issues/92.
 */
@RunWith(AndroidJUnit4::class)
class BatteryScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(state: BatteryState) {
        composeTestRule.setContent {
            GadgetTestTheme {
                BatteryScreenContent(
                    state = state,
                    moduleInfo = null,
                )
            }
        }
    }

    @Test
    fun rendersScreenTitleAndStatusCard() {
        setContent(
            BatteryState(
                level = 78,
                isCharging = true,
                chargingStatus = BatteryChargingStatus.Charging,
                pluggedType = BatteryPlugType.USB,
                health = BatteryHealth.Good,
                temperatureCelsius = 28.5f,
                voltageMv = 4120,
                isAvailable = true,
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.battery_screen_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.battery_card_status_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.battery_card_charging_title))
            .assertIsDisplayed()
    }

    @Test
    fun unavailableStateShowsUnavailableMessage() {
        setContent(BatteryState(isAvailable = false))
        composeTestRule.onNodeWithText(res.getString(R.string.battery_screen_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.battery_unavailable))
            .assertIsDisplayed()
    }
}
