package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetChip], [GadgetBadge], and
 * [GadgetStatusDot]:
 *
 *   - Chip selection state surfaces through the a11y tree
 *     (`assertIsSelected` / `assertIsNotSelected`).
 *   - Chip onClick fires when tapped.
 *   - Badge with text renders the text node.
 *   - StatusDot renders without crash (purely visual; no a11y).
 */
class StatusIndicatorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chip_unselected_reportsNotSelected() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetChip(selected = false, onClick = {}, label = "All")
            }
        }
        composeTestRule.onNodeWithText("All").assertIsNotSelected()
    }

    @Test
    fun chip_selected_reportsSelected() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetChip(selected = true, onClick = {}, label = "Active")
            }
        }
        composeTestRule.onNodeWithText("Active").assertIsSelected()
    }

    @Test
    fun chip_click_fires() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetChip(
                    selected = false,
                    onClick = { clicked = true },
                    label = "Archived",
                )
            }
        }
        composeTestRule.onNodeWithText("Archived").performClick()
        assertTrue("Chip onClick must fire when tapped.", clicked)
    }

    @Test
    fun badge_withText_rendersText() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetBadge(text = "99+")
            }
        }
        composeTestRule.onNodeWithText("99+").assertExists()
    }

    @Test
    fun statusDot_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetStatusDot(
                    contentDescription = "Online",
                    modifier = Modifier.testTag(DotTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(DotTag).assertExists()
    }

    private companion object {
        const val DotTag = "status-dot"
    }
}
