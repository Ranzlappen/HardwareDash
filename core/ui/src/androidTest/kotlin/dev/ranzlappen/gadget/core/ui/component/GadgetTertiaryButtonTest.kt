package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetTertiaryButton] (ghost). Same two
 * checks as the other labelled buttons; the visual difference
 * (no container fill) is captured in previews / screenshot tests
 * rather than here.
 */
class GadgetTertiaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetTertiaryButton(
                    onClick = { clicked = true },
                    text = "Learn more",
                )
            }
        }
        composeTestRule.onNodeWithText("Learn more")
            .assertIsEnabled()
            .performClick()
        assertTrue("Expected onClick to fire after a tap.", clicked)
    }

    @Test
    fun disabled_suppressesClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetTertiaryButton(
                    onClick = { clicked = true },
                    text = "Learn more",
                    enabled = false,
                )
            }
        }
        composeTestRule.onNodeWithText("Learn more")
            .assertIsNotEnabled()
            .performClick()
        assertFalse("Disabled tertiary button must not fire onClick.", clicked)
    }
}
