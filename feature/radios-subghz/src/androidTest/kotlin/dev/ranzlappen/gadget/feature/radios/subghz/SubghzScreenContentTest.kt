package dev.ranzlappen.gadget.feature.radios.subghz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [SubghzScreenContent] — Hilt-free
 * (the monitors slot defaults to a no-op). Mirror of
 * `WifiScreenContentTest` / `SensorsScreenContentTest`; runs via
 * `:feature:radios-subghz:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class SubghzScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun connectedBridge_rendersDeviceDetails() {
        composeTestRule.setContent {
            GadgetTestTheme {
                SubghzScreenContent(
                    state = SubghzState(
                        usbHostAvailable = true,
                        device = SdrDevice.YardStickOne,
                    ),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule.onNodeWithText("Bridge attached").assertIsDisplayed()
        composeTestRule.onNodeWithText(SdrDevice.YardStickOne.displayName).assertIsDisplayed()
    }

    @Test
    fun noBridge_rendersGuidance() {
        composeTestRule.setContent {
            GadgetTestTheme {
                SubghzScreenContent(
                    state = SubghzState(usbHostAvailable = true, device = null),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule.onNodeWithText("No bridge").assertIsDisplayed()
    }

    @Test
    fun noUsbHost_rendersUnavailable() {
        composeTestRule.setContent {
            GadgetTestTheme {
                SubghzScreenContent(
                    state = SubghzState(usbHostAvailable = false, device = null),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule
            .onNodeWithText(
                "This device has no USB host bus, so an external Sub-GHz radio cannot be attached",
            )
            .assertIsDisplayed()
    }
}
