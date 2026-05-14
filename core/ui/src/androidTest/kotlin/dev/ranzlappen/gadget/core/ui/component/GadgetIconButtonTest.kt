package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetIconButton]. Beyond the standard
 * click + disabled pair, also locks in that [contentDescription]
 * surfaces through the a11y tree so screen readers announce the
 * button.
 */
class GadgetIconButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetIconButton(
                    onClick = { clicked = true },
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search sensors",
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Search sensors")
            .assertIsEnabled()
            .performClick()
        assertTrue("Expected onClick to fire after a tap.", clicked)
    }

    @Test
    fun disabled_suppressesClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetIconButton(
                    onClick = { clicked = true },
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search sensors",
                    enabled = false,
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Search sensors")
            .assertIsNotEnabled()
            .performClick()
        assertFalse("Disabled icon button must not fire onClick.", clicked)
    }

    @Test
    fun contentDescription_propagatesToA11yTree() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetIconButton(
                    onClick = {},
                    icon = Icons.Outlined.Search,
                    contentDescription = "Custom label that screen readers will announce",
                )
            }
        }
        // The assertion succeeds iff the node exists with the supplied
        // description — verifies the description reaches the
        // semantics tree via the inner Icon().
        composeTestRule
            .onNodeWithContentDescription("Custom label that screen readers will announce")
            .assertIsEnabled()
    }
}
