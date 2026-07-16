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
 * Exercises the stateless home screen with a curated entry list (and the
 * empty case) and asserts the chrome (title + edit affordance) plus a module
 * tile. Mirrors the torch reference pattern (`createComposeRule` +
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

    private fun setContent(entries: List<DashboardEntry>) {
        composeTestRule.setContent {
            GadgetTestTheme {
                DashboardScreenContent(
                    entries = entries,
                    onNavigate = {},
                    onEdit = {},
                )
            }
        }
    }

    @Test
    fun rendersEditAffordanceAndModuleTiles() {
        setContent(
            listOf(
                DashboardEntry(destination = GadgetDestination.Torch, hidden = false, pinned = true),
                DashboardEntry(destination = GadgetDestination.Vibration, hidden = false, pinned = false),
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.dashboard_edit))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(GadgetDestination.Torch.label)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun emptyEntriesShowsEmptyState() {
        setContent(emptyList())
        composeTestRule.onNodeWithText(res.getString(R.string.dashboard_edit))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.dashboard_empty))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
