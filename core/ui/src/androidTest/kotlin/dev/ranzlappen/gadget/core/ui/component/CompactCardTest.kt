package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [CompactCard]:
 *
 *   - title + subtitle render as visible text.
 *   - Supplying [onClick] makes the card a clickable surface.
 *   - Omitting [onClick] leaves the card with no click action (the
 *     a11y tree sees a static info row, not a button).
 */
class CompactCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun titleAndSubtitle_render() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CompactCard(
                    title = "Battery",
                    subtitle = "87% · charging",
                    leadingIcon = Icons.Outlined.BatteryFull,
                )
            }
        }
        composeTestRule.onNodeWithText("Battery").assertHasNoClickAction()
        composeTestRule.onNodeWithText("87% · charging").assertHasNoClickAction()
    }

    @Test
    fun onClick_isClickable_andFires() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                CompactCard(
                    title = "Wi-Fi",
                    subtitle = "Connected",
                    onClick = { clicked = true },
                    modifier = Modifier.testTag(ClickableTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(ClickableTag)
            .assertHasClickAction()
            .performClick()
        assertTrue("CompactCard with onClick must fire the callback.", clicked)
    }

    @Test
    fun withoutOnClick_isNotClickable() {
        composeTestRule.setContent {
            GadgetTestTheme {
                CompactCard(
                    title = "Static row",
                    subtitle = "Read-only",
                    modifier = Modifier.testTag(StaticTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(StaticTag)
            .assertHasNoClickAction()
    }

    private companion object {
        const val ClickableTag = "compact-card-clickable"
        const val StaticTag = "compact-card-static"
    }
}
