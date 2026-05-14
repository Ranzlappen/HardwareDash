package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetFab] across both variants:
 *
 *   - Icon-only FAB (no [text]) — circular 56 dp surface, label
 *     reachable only via [contentDescription].
 *   - Extended FAB (with [text]) — icon + label inline, label
 *     reachable via either contentDescription or visible text.
 */
class GadgetFabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun iconOnly_click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetFab(
                    onClick = { clicked = true },
                    icon = Icons.Outlined.Add,
                    contentDescription = "Add sensor",
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Add sensor")
            .assertIsEnabled()
            .performClick()
        assertTrue("Expected icon-only FAB onClick to fire.", clicked)
    }

    @Test
    fun extended_click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetFab(
                    onClick = { clicked = true },
                    icon = Icons.Outlined.Add,
                    contentDescription = "Add sensor",
                    text = "Add sensor",
                )
            }
        }
        composeTestRule.onNodeWithText("Add sensor")
            .performClick()
        assertTrue("Expected extended FAB onClick to fire.", clicked)
    }

    @Test
    fun extended_rendersVisibleLabel() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetFab(
                    onClick = {},
                    icon = Icons.Outlined.Add,
                    contentDescription = "Add",
                    text = "Add new sensor",
                )
            }
        }
        composeTestRule.onNodeWithText("Add new sensor")
            .assertIsEnabled()
    }
}
