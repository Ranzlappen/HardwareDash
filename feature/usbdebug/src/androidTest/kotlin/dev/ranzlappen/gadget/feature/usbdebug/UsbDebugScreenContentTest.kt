package dev.ranzlappen.gadget.feature.usbdebug

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [UsbDebugScreenContent] — exercised
 * with curated [UsbDebugState] snapshots, capturing dispatched
 * [UsbDebugUiEvent]s. Hilt-free (the monitor slots default to no-ops).
 * Mirrors `TorchScreenContentTest` / `VibrationScreenContentTest`. Runs via
 * `:feature:usbdebug:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class UsbDebugScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: UsbDebugState,
        events: MutableList<UsbDebugUiEvent> = mutableListOf(),
    ): MutableList<UsbDebugUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                UsbDebugScreenContent(
                    state = state,
                    onEvent = { events += it },
                    moduleInfo = null,
                )
            }
        }
        return events
    }

    @Test
    fun rendersScreenTitle() {
        setContent(UsbDebugState.Initial)
        composeTestRule.onNodeWithText(res.getString(R.string.usbdebug_screen_title)).assertIsDisplayed()
    }

    @Test
    fun standardFlavorShowsEnabledChip() {
        setContent(UsbDebugState.Initial.copy(isRootedFlavor = false, usbDebuggingEnabled = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_status_chip_enabled))
            .assertIsDisplayed()
    }

    @Test
    fun standardFlavorShowsDisabledChip() {
        setContent(UsbDebugState.Initial.copy(isRootedFlavor = false, usbDebuggingEnabled = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_status_chip_disabled))
            .assertIsDisplayed()
    }

    @Test
    fun openDeveloperOptionsDispatchesEvent() {
        val events = setContent(UsbDebugState.Initial.copy(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_open_developer_options))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(UsbDebugUiEvent.OpenDeveloperOptions))
    }

    @Test
    fun rootedFlavorShowsFunctionPickerCard() {
        setContent(UsbDebugState.Initial.copy(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_function_card_title))
            .assertIsDisplayed()
    }

    @Test
    fun tappingAFunctionChipDispatchesSelectFunction() {
        val events = setContent(UsbDebugState.Initial.copy(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_function_chip_rndis))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(UsbDebugUiEvent.SelectFunction(UsbFunctionType.RNDIS)))
    }

    @Test
    fun diagnosticsPanelToggleDispatchesEvent() {
        val events = setContent(UsbDebugState.Initial.copy(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_diagnostics_panel_title))
            .performScrollTo()
            .performClick()
        assertTrue(events.contains(UsbDebugUiEvent.DiagnosticsToggle))
    }

    @Test
    fun expandedDiagnosticsPanelShowsDumpExcerptAndSource() {
        setContent(
            UsbDebugState.Initial.copy(
                isRootedFlavor = true,
                diagnosticsExpanded = true,
                usbDump = UsbDumpPanelState(excerpt = "Device: mtp", source = "dumpsys usb"),
            ),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_diagnostics_source, "dumpsys usb"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun standardFlavorHidesRootedOnlyCards() {
        setContent(UsbDebugState.Initial.copy(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.usbdebug_function_card_title))
            .assertDoesNotExist()
    }
}
