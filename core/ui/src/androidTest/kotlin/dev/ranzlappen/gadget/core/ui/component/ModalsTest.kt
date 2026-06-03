package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for [GadgetDialog]:
 *
 *   - Title + body text render.
 *   - Confirm button slot fires its callback when tapped.
 *   - Dismiss button slot (when present) fires its callback.
 *
 * Behaviour contract for [GadgetBottomSheet] (closes #91):
 *
 *   - Title + slotted content render.
 *   - The title node carries `heading()` semantics (the a11y contract).
 *   - Actions inside the content slot fire their callbacks.
 *   - Conditional placement (the documented show/hide pattern) removes
 *     the sheet when the caller's visibility flag flips false.
 *
 * `ModalBottomSheet` renders into its own window, but the Compose test
 * framework traverses all owned windows, so the ui-test-manifest's
 * stock `ComponentActivity` (via [createComposeRule]) hosts it without a
 * bespoke sheet-host activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ModalsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_rendersTitleAndBody() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        GadgetPrimaryButton(onClick = {}, text = "OK")
                    },
                    title = "Delete sensor",
                    text = "This action cannot be undone. The sensor and its history will be removed permanently.",
                )
            }
        }
        composeTestRule.onNodeWithText("Delete sensor").assertExists()
        composeTestRule.onNodeWithText(
            "This action cannot be undone. The sensor and its history will be removed permanently.",
        ).assertExists()
    }

    @Test
    fun dialog_confirmButton_fires() {
        var confirmed = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        GadgetPrimaryButton(
                            onClick = { confirmed = true },
                            text = "Confirm delete",
                        )
                    },
                    title = "Delete sensor",
                )
            }
        }
        composeTestRule.onNodeWithText("Confirm delete").performClick()
        assertTrue("Confirm button must fire its callback.", confirmed)
    }

    @Test
    fun dialog_dismissButton_fires() {
        var dismissed = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        GadgetPrimaryButton(onClick = {}, text = "Delete")
                    },
                    dismissButton = {
                        GadgetTertiaryButton(
                            onClick = { dismissed = true },
                            text = "Cancel",
                        )
                    },
                    title = "Delete sensor",
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue("Dismiss button must fire its callback.", dismissed)
    }

    // ─── GadgetBottomSheet ──────────────────────────────────────────

    @Test
    fun sheet_rendersTitleAndContent() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetBottomSheet(
                    onDismissRequest = {},
                    title = "Configure widget",
                ) {
                    Text("Pick an icon and a background.")
                }
            }
        }
        composeTestRule.onNodeWithText("Configure widget").assertExists()
        composeTestRule.onNodeWithText("Pick an icon and a background.").assertExists()
    }

    @Test
    fun sheet_title_hasHeadingSemantics() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetBottomSheet(
                    onDismissRequest = {},
                    title = "Configure widget",
                ) {
                    Text("body")
                }
            }
        }
        // The a11y contract: the title is announced as a heading so
        // TalkBack emits a heading earcon before reading it.
        composeTestRule.onNodeWithText("Configure widget")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun sheet_contentAction_fires() {
        var saved = false
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetBottomSheet(
                    onDismissRequest = {},
                    title = "Configure widget",
                ) {
                    GadgetPrimaryButton(onClick = { saved = true }, text = "Save widget")
                }
            }
        }
        composeTestRule.onNodeWithText("Save widget").performClick()
        assertTrue("Content slot action must fire its callback.", saved)
    }

    @Test
    fun sheet_conditionalPlacement_hidesContent() {
        // Exercises the documented show/hide contract: the caller owns
        // visibility and places the sheet conditionally in the tree.
        composeTestRule.setContent {
            GadgetTestTheme {
                var visible by remember { mutableStateOf(true) }
                if (visible) {
                    GadgetBottomSheet(
                        onDismissRequest = { visible = false },
                        title = "Configure widget",
                    ) {
                        GadgetTertiaryButton(
                            onClick = { visible = false },
                            text = "Close sheet",
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Configure widget").assertExists()
        composeTestRule.onNodeWithText("Close sheet").performClick()
        composeTestRule.onNodeWithText("Configure widget").assertDoesNotExist()
    }
}
