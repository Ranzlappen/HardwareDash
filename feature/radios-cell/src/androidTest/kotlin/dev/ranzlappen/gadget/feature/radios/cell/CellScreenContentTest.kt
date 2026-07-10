package dev.ranzlappen.gadget.feature.radios.cell

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [CellScreenContent] — Hilt-free
 * (the ViewModel, permission plumbing, and monitor slots are supplied by
 * [CellScreen] and default to no-ops here). Mirrors
 * `SensorsScreenContentTest` / `GpsScreenContent`'s preview shapes; runs via
 * `:feature:radios-cell:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class CellScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionGranted_rendersStatusReadout() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(
                        permissionGranted = true,
                        simState = SimStateUi.Ready,
                        carrierName = "Example Mobile",
                        networkType = CellNetworkType.Lte4G,
                        signalLevel = 3,
                    ),
                    isRootedFlavor = false,
                    moduleInfo = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Example Mobile").assertIsDisplayed()
        composeTestRule.onNodeWithText("4G LTE").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 of 4 bars").assertIsDisplayed()
    }

    @Test
    fun permissionNotGranted_rendersPermissionCard() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(permissionGranted = false),
                    isRootedFlavor = false,
                    moduleInfo = null,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Grant Phone state to see carrier, network type, and live signal strength.")
            .assertIsDisplayed()
    }

    @Test
    fun requestPermissionButton_invokesCallback() {
        var requested = false
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(permissionGranted = false),
                    isRootedFlavor = false,
                    moduleInfo = null,
                    onRequestPermission = { requested = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Grant permission").performClick()
        assert(requested)
    }

    @Test
    fun rootedFlavor_rendersRawModemDumpPanel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(permissionGranted = true, simState = SimStateUi.Ready),
                    isRootedFlavor = true,
                    moduleInfo = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Raw modem diagnostics").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rootedFlavor_loadedDump_rendersNodeRows() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(permissionGranted = true, simState = SimStateUi.Ready),
                    isRootedFlavor = true,
                    moduleInfo = null,
                    rawModemDump = CellDumpUiState.Loaded(
                        mapOf("/sys/class/qcom_smd8/status" to "online"),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithText("/sys/class/qcom_smd8/status")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun standardFlavor_omitsRootedDumpPanel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CellScreenContent(
                    state = CellState(permissionGranted = true, simState = SimStateUi.Ready),
                    isRootedFlavor = false,
                    moduleInfo = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Raw modem diagnostics").assertDoesNotExist()
    }
}
