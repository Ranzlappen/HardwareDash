package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [DashboardScreenContent].
 *
 * Exercises the stateless Home screen (v3 B3) with a fixed [HomeStatus] and
 * pinned list (and the no-pins case): status cards, the pinned tool and the
 * edit affordance. Mirrors the torch reference pattern (`createComposeRule` +
 * [GadgetTestTheme]).
 *
 * Tests run via `:feature:dashboard:connectedDebugAndroidTest`. CI emulator
 * workflow tracked at https://github.com/Ranzlappen/HardwareDash/issues/92.
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        pinned: List<DashboardEntry>,
        status: HomeStatus = HomeStatus(batteryPercent = 87, wifiEnabled = false, torchOn = false),
    ) {
        composeTestRule.setContent {
            GadgetTestTheme {
                DashboardScreenContent(
                    status = status,
                    pinned = pinned,
                    onNavigate = {},
                    onToggleTorch = {},
                    onBuzz = {},
                    onEdit = {},
                )
            }
        }
    }

    @Test
    fun rendersStatusQuickActionsAndPinnedTools() {
        setContent(
            listOf(DashboardEntry(destination = GadgetDestination.Apps, hidden = false, pinned = true)),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.home_battery))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.home_wifi_off))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(GadgetDestination.Apps.label)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.dashboard_edit))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun noPinnedToolsShowsHint() {
        setContent(emptyList())
        composeTestRule.onNodeWithText(res.getString(R.string.home_pinned_empty))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
