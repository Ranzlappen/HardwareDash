package dev.ranzlappen.gadget.feature.torch.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionBehavior
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationResult
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the torch [WidgetConfigurationSheet] shell over the
 * kit-generic `WidgetCustomizationSheet`. Gated on the CI emulator workflow at
 * https://github.com/Ranzlappen/HardwareDash/issues/92.
 *
 * The torch shell only maps a [TorchWidgetConfig] into the kit sheet's
 * `initial*` params and the [WidgetCustomizationResult] back out — these tests
 * confirm the right footer label paints (new vs edit) and that confirm fires
 * with the bound function's action key.
 */
@RunWith(AndroidJUnit4::class)
class WidgetConfigurationSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    // A small stand-in for the real TorchWidgetFunctionCatalog list.
    private val functions = listOf(
        WidgetFunction(
            id = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
            label = "Flashlight",
            behavior = WidgetFunctionBehavior.Toggle("torch_on", "torch_off", "torch_power"),
        ),
        WidgetFunction(
            id = TorchWidgetConfig.FUNCTION_STROBE,
            label = "Strobe",
            params = listOf(ActionParam("rate_hz", ActionParamType.Float, "5", 1f, 20f)),
            behavior = WidgetFunctionBehavior.Toggle("strobe_start", "strobe_stop", "strobe_running"),
        ),
    )

    private val flashlightInitial = TorchWidgetConfig(
        displayName = "Test flashlight",
        actionKey = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
    )

    @Test
    fun newSheetShowsCreateLabel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = flashlightInitial,
                    isExisting = false,
                    functions = functions,
                    onDismiss = {},
                    onConfirm = {},
                    resolveIcon = { WidgetIconSource.Resource(R.drawable.ic_flashlight_on) },
                    onImportCustomIcon = { null },
                    iconChoices = emptyList(),
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(WidgetKitR.string.widget_kit_create))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun editSheetShowsSaveLabel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = flashlightInitial,
                    isExisting = true,
                    functions = functions,
                    onDismiss = {},
                    onConfirm = {},
                    resolveIcon = { WidgetIconSource.Resource(R.drawable.ic_flashlight_on) },
                    onImportCustomIcon = { null },
                    iconChoices = emptyList(),
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(WidgetKitR.string.widget_kit_save))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun functionPickerListsTheBoundFunctions() {
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = flashlightInitial,
                    isExisting = false,
                    functions = functions,
                    onDismiss = {},
                    onConfirm = {},
                    resolveIcon = { WidgetIconSource.Resource(R.drawable.ic_flashlight_on) },
                    onImportCustomIcon = { null },
                    iconChoices = emptyList(),
                )
            }
        }

        // With more than one function the picker renders a chip per function.
        composeTestRule.onNodeWithText("Strobe").assertIsDisplayed()
    }

    @Test
    fun confirmCallbackReceivesResultWithSelectedActionKey() {
        var captured: WidgetCustomizationResult? = null
        composeTestRule.setContent {
            GadgetTestTheme {
                WidgetConfigurationSheet(
                    initial = flashlightInitial,
                    isExisting = false,
                    functions = functions,
                    onDismiss = {},
                    onConfirm = { captured = it },
                    resolveIcon = { WidgetIconSource.Resource(R.drawable.ic_flashlight_on) },
                    onImportCustomIcon = { null },
                    iconChoices = emptyList(),
                )
            }
        }

        composeTestRule
            .onNodeWithText(res.getString(WidgetKitR.string.widget_kit_create))
            .performScrollTo()
            .performClick()

        assertNotNull(captured)
        assertEquals(TorchWidgetConfig.FUNCTION_FLASHLIGHT, captured!!.actionKey)
    }
}
