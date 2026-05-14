package dev.ranzlappen.gadget.feature.torch.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Instrumented tests for [WidgetConfigurationSheet]. Gated on the CI
 * emulator workflow at
 * https://github.com/Ranzlappen/HardwareDash/issues/92.
 */
@RunWith(AndroidJUnit4::class)
class WidgetConfigurationSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private val strobeInitial = TorchWidgetConfig(
        type = WidgetType.Strobe,
        displayName = "Test strobe",
        rateHz = 5f,
        sosMode = false,
    )

    private val flashlightInitial = TorchWidgetConfig(
        type = WidgetType.Flashlight,
        displayName = "Test flashlight",
    )

    @Test
    fun newStrobeSheetShowsCreateLabel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = strobeInitial,
                    isExisting = false,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(R.string.torch_widget_config_save_new))
            .assertIsDisplayed()
    }

    @Test
    fun editStrobeSheetShowsSaveLabel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = strobeInitial,
                    isExisting = true,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(R.string.torch_widget_config_save_existing))
            .assertIsDisplayed()
    }

    @Test
    fun strobeSheetExposesSosToggle() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = strobeInitial,
                    isExisting = false,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(R.string.torch_widget_config_sos_label))
            .assertIsDisplayed()
    }

    @Test
    fun flashlightSheetHidesSosToggle() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = flashlightInitial,
                    isExisting = false,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        // Compose's onNodeWithText returns an unattached node when the
        // text is absent; calling `fetchSemanticsNode()` on such a node
        // would throw. Using `assertExists` would also throw. We
        // confirm absence via a try/catch on `assertIsDisplayed`.
        var failed = false
        try {
            composeTestRule
                .onNodeWithText(res.getString(R.string.torch_widget_config_sos_label))
                .assertIsDisplayed()
        } catch (_: AssertionError) {
            failed = true
        }
        assertEquals(true, failed, "SOS toggle should NOT render for flashlight widgets")
    }

    @Test
    fun confirmCallbackReceivesUpdatedConfig() {
        var captured: TorchWidgetConfig? = null
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = strobeInitial,
                    isExisting = false,
                    onDismiss = {},
                    onConfirm = { captured = it },
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(R.string.torch_widget_config_save_new))
            .performClick()

        assertNotNull(captured)
        assertEquals(strobeInitial.type, captured!!.type)
    }
}
