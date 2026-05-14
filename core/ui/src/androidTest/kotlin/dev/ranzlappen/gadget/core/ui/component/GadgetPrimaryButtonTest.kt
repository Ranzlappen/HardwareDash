package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Smoke tests for [GadgetPrimaryButton]. Validates the test
 * infrastructure end-to-end (`:core:testing` → `androidTest` source
 * set → Compose UI test runtime) and locks in the three behaviour
 * contracts every button variant must honour:
 *
 *   1. Click in the idle state fires the [onClick] callback.
 *   2. `enabled = false` reports the button as disabled to the a11y
 *      tree and suppresses click delivery.
 *   3. `loading = true` swaps the label for a progress indicator and
 *      suppresses click delivery (even if the consumer manages to
 *      hit the surface).
 *
 * The remaining button variants (`Secondary`, `Tertiary`, `Icon`,
 * `Fab`) share the same private `GlassyLabelledButton` /
 * `IconButton` / `Fab` plumbing, so this smoke test covers the
 * shared paths. Full per-variant coverage lands in batches X7b/c.
 */
class GadgetPrimaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun click_emitsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetPrimaryButton(
                    onClick = { clicked = true },
                    text = "Save",
                )
            }
        }

        composeTestRule.onNodeWithText("Save")
            .assertIsEnabled()
            .performClick()

        assertTrue("Expected onClick to fire after a tap.", clicked)
    }

    @Test
    fun disabled_suppressesClick() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetPrimaryButton(
                    onClick = { clicked = true },
                    text = "Save",
                    enabled = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Save")
            .assertIsNotEnabled()
            .performClick()

        assertFalse("Disabled button must not fire onClick.", clicked)
    }

    @Test
    fun loading_suppressesClickAndHidesLabel() {
        var clicked = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetPrimaryButton(
                    onClick = { clicked = true },
                    text = "Save",
                    loading = true,
                    modifier = Modifier.testTag(LoadingButtonTag),
                )
            }
        }

        // While loading, the label is replaced by a CircularProgressIndicator.
        composeTestRule.onNodeWithText("Save")
            .assertDoesNotExist()

        // Surface still exists and is reachable by tag; clicking must be a no-op.
        composeTestRule.onNodeWithTag(LoadingButtonTag)
            .assert(hasClickAction())
            .performClick()

        assertFalse("Loading button must not fire onClick.", clicked)
    }

    private companion object {
        const val LoadingButtonTag = "primary-button-loading"
    }
}
