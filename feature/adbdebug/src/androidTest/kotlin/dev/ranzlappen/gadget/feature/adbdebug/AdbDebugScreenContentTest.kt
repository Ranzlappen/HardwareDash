package dev.ranzlappen.gadget.feature.adbdebug

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbSetPropAllowList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [AdbDebugScreenContent].
 *
 * Exercises curated [AdbDebugState] snapshots for both the standard tier
 * (read-only readout, no rooted cards) and the rooted tier (toggle / network
 * / dump / setprop cards), and asserts every dispatched [AdbDebugUiEvent]
 * into a captured list — the same pattern `TorchScreenContentTest` uses.
 *
 * Tests run via `:feature:adbdebug:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AdbDebugScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: AdbDebugState,
        events: MutableList<AdbDebugUiEvent> = mutableListOf(),
    ): MutableList<AdbDebugUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                AdbDebugScreenContent(
                    state = state,
                    moduleInfo = null,
                    onEvent = { events += it },
                )
            }
        }
        return events
    }

    @Test
    fun standardTierShowsStateCardButNoRootedCards() {
        setContent(AdbDebugState(isRootedFlavor = false, adbEnabled = true))

        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_state_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_chip_enabled))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_toggle_title))
            .assertDoesNotExist()
    }

    @Test
    fun standardTierRendersDisabledChipWhenAdbIsOff() {
        setContent(AdbDebugState(isRootedFlavor = false, adbEnabled = false))

        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_chip_disabled))
            .assertIsDisplayed()
    }

    @Test
    fun rootedTierShowsToggleCard() {
        setContent(AdbDebugState(isRootedFlavor = true, adbEnabled = true))

        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_toggle_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_network_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_dump_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_card_setprop_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun dumpButtonDispatchesDumpPropertiesEvent() {
        val events = setContent(AdbDebugState(isRootedFlavor = true, adbEnabled = true))

        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_dump_action))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(AdbDebugUiEvent.DumpProperties), events)
    }

    @Test
    fun applySetPropButtonDispatchesApplySetPropEvent() {
        val events = setContent(AdbDebugState(isRootedFlavor = true, adbEnabled = true))

        composeTestRule.onNodeWithText(res.getString(R.string.adbdebug_setprop_apply))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(AdbDebugUiEvent.ApplySetProp), events)
    }

    @Test
    fun setPropKeyChipDispatchesSetPropKeyChangeEvent() {
        val key = AdbSetPropAllowList.EXACT_KEYS[1]
        val events = setContent(
            AdbDebugState(isRootedFlavor = true, adbEnabled = true, setPropKey = "unused-placeholder"),
        )

        composeTestRule.onNodeWithText(key).performScrollTo().performClick()

        assertEquals(listOf(AdbDebugUiEvent.SetPropKeyChange(key)), events)
    }

    @Test
    fun lastActionMessageIsDisplayedWhenPresent() {
        setContent(
            AdbDebugState(isRootedFlavor = true, adbEnabled = true, lastActionMessage = "ADB enabled"),
        )

        composeTestRule.onNodeWithText("ADB enabled").assertIsDisplayed()
    }
}
