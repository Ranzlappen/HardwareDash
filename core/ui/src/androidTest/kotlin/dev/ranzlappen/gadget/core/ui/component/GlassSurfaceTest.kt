package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material3.Text
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
 * Behaviour contract for [GlassSurface]:
 *
 *   - Content slot renders inside the glass container.
 *   - Supplying [onClick] makes the whole surface clickable.
 *   - Omitting [onClick] leaves the surface non-interactive.
 */
class GlassSurfaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun content_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GlassSurface {
                    Text(text = "Inside the glass")
                }
            }
        }
        composeTestRule.onNodeWithText("Inside the glass")
            .assertHasNoClickAction()
    }

    @Test
    fun onClick_isClickable_andFires() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GlassSurface(
                    modifier = Modifier.testTag(ClickableTag),
                    onClick = { clicked = true },
                ) {
                    Text(text = "Clickable glass")
                }
            }
        }
        composeTestRule.onNodeWithTag(ClickableTag)
            .assertHasClickAction()
            .performClick()
        assertTrue("GlassSurface with onClick must fire the callback.", clicked)
    }

    @Test
    fun withoutOnClick_isNotClickable() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GlassSurface(modifier = Modifier.testTag(StaticTag)) {
                    Text(text = "Static glass")
                }
            }
        }
        composeTestRule.onNodeWithTag(StaticTag)
            .assertHasNoClickAction()
    }

    private companion object {
        const val ClickableTag = "glass-surface-clickable"
        const val StaticTag = "glass-surface-static"
    }
}
