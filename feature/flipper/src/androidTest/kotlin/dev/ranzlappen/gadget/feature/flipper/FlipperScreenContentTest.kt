package dev.ranzlappen.gadget.feature.flipper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [FlipperScreenContent] — Hilt-free
 * (the monitors slot defaults to a no-op). Mirror of the other module
 * content tests; runs via `:feature:flipper:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class FlipperScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun connected_rendersDeviceDetails() {
        composeTestRule.setContent {
            GadgetTestTheme {
                FlipperScreenContent(
                    state = FlipperUiState(
                        connection = FlipperConnectionManager.State.Connected(
                            transport = "USB",
                            deviceName = "Flipper Wabbit",
                            firmwareVersion = "0.103.1",
                            batteryPercent = 87,
                        ),
                    ),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule.onNodeWithText("Flipper Wabbit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect").assertIsDisplayed()
    }

    @Test
    fun disconnected_rendersConnectActions() {
        composeTestRule.setContent {
            GadgetTestTheme {
                FlipperScreenContent(
                    state = FlipperUiState(connection = FlipperConnectionManager.State.Disconnected),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule.onNodeWithText("Connect USB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect Bluetooth").assertIsDisplayed()
    }

    @Test
    fun failed_rendersReason() {
        composeTestRule.setContent {
            GadgetTestTheme {
                FlipperScreenContent(
                    state = FlipperUiState(
                        connection = FlipperConnectionManager.State.Failed(
                            "No Flipper Zero attached via USB",
                        ),
                    ),
                    moduleInfo = null,
                )
            }
        }
        composeTestRule
            .onNodeWithText("Connection failed: No Flipper Zero attached via USB")
            .assertIsDisplayed()
    }
}
