package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetEmptyState]:
 *
 *   - [title] is required and always renders.
 *   - [subtitle] renders when non-null.
 *   - [action] slot, when supplied, is hooked up so its click fires.
 *   - Nulling [subtitle] / [action] omits them entirely (no empty
 *     spacer left behind).
 */
class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun title_alwaysRenders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetEmptyState(title = "No sensors yet")
            }
        }
        composeTestRule.onNodeWithText("No sensors yet").assertExists()
    }

    @Test
    fun subtitle_rendersWhenPresent() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetEmptyState(
                    title = "No sensors yet",
                    subtitle = "Add your first sensor to begin.",
                )
            }
        }
        composeTestRule.onNodeWithText("Add your first sensor to begin.")
            .assertExists()
    }

    @Test
    fun action_slot_isReachable_andFires() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetEmptyState(
                    title = "No sensors yet",
                    icon = Icons.Outlined.Sensors,
                    action = {
                        GadgetPrimaryButton(
                            onClick = { clicked = true },
                            text = "Add sensor",
                        )
                    },
                )
            }
        }
        composeTestRule.onNodeWithText("Add sensor").performClick()
        assertTrue("Action slot button click must fire its callback.", clicked)
    }

    @Test
    fun nullSubtitleAndAction_renderTitleOnly() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetEmptyState(title = "Just a title")
            }
        }
        composeTestRule.onNodeWithText("Just a title").assertExists()
        // Nothing else to assert positively; the test passes if the
        // composition succeeds without throwing on the missing slots.
    }
}
