package dev.ranzlappen.gadget.feature.sensors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [SensorsScreenContent] — curated
 * [SensorRowUi] snapshots, Hilt-free (the monitors slot defaults to a
 * no-op). Mirror of `TorchScreenContentTest` / `VibrationScreenContentTest`;
 * runs via `:feature:sensors:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class SensorsScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val rows = listOf(
        SensorRowUi("proximity", "Proximity", "cm", available = true, value = 4.2f),
        SensorRowUi("light", "Ambient light", "lx", available = true, value = null),
        SensorRowUi("acceleration", "Acceleration", "m/s²", available = false, value = null),
    )

    private fun setContent() {
        composeTestRule.setContent {
            GadgetTestTheme {
                SensorsScreenContent(rows = rows, moduleInfo = null)
            }
        }
    }

    @Test
    fun rendersOneCardPerSignal() {
        setContent()
        composeTestRule.onNodeWithText("Proximity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ambient light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acceleration").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun liveValue_rendersWithUnit() {
        setContent()
        composeTestRule.onNodeWithText("4.2 cm").assertIsDisplayed()
    }

    @Test
    fun pendingAndAbsentStates_render() {
        setContent()
        composeTestRule.onNodeWithText("Waiting for reading…").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Not present on this device")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
