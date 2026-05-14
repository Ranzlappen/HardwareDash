package dev.ranzlappen.gadget.core.ui.component

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
 * [GadgetBottomSheet] is intentionally not covered here — sheets
 * require `SheetState` orchestration through a host activity that
 * supports back-press handling, which is awkward to set up in a
 * unit-scale UI test. A future batch with a dedicated sheet-host
 * test activity can pick that up.
 */
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
}
