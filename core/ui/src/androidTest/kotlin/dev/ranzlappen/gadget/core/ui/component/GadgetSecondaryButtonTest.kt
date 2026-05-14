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
 * Behaviour contract for [GadgetSecondaryButton]. The button shares
 * the private `GlassyLabelledButton` helper with Primary / Tertiary
 * so the click + disabled paths are inherited; this test re-verifies
 * them at the public API boundary so a future refactor that moves
 * Secondary off the shared helper still gates on these contracts.
 */
class GadgetSecondaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetSecondaryButton(
                    onClick = { clicked = true },
                    text = "Cancel",
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel")
            .assertIsEnabled()
            .performClick()
        assertTrue("Expected onClick to fire after a tap.", clicked)
    }

    @Test
    fun disabled_suppressesClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetSecondaryButton(
                    onClick = { clicked = true },
                    text = "Cancel",
                    enabled = false,
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel")
            .assertIsNotEnabled()
            .performClick()
        assertFalse("Disabled secondary button must not fire onClick.", clicked)
    }
}
