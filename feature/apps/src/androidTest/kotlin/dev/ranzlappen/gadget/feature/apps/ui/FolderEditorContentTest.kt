package dev.ranzlappen.gadget.feature.apps.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.rules.FolderRuleSet
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Hilt-free [FolderEditorContent].
 *
 * Renders with an empty app catalog so the `AppIcon` rows (which reach the
 * app's Hilt graph via `EntryPointAccessors`) are never composed — the rest of
 * the editor is pure state + callbacks. Asserts the top sections render and the
 * back affordance fires its callback.
 *
 * Runs via `:feature:apps:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class FolderEditorContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun state(folderName: String) = FolderEditorState(
        folder = Folder(
            id = 1L,
            name = folderName,
            baseColorArgb = 0xFF6750A4.toInt(),
            coverIcon = "auto",
            sortOrder = 0,
            locked = false,
            createdAt = 0L,
        ),
        filteredApps = emptyList(),
        membership = emptySet(),
        ruleSet = FolderRuleSet(),
        searchQuery = "",
        otherFolderMembership = emptyMap(),
    )

    private fun setContent(
        folderName: String = "Games",
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            GadgetTestTheme {
                FolderEditorContent(
                    state = state(folderName),
                    onBack = onBack,
                    onRename = {},
                    onSetColor = {},
                    onSetLocked = {},
                    onPinToHome = { false },
                    onToggleMember = {},
                    onAddWebLink = { _, _ -> },
                    onSetCoverSymbol = {},
                    onClearCover = {},
                    onSetCoverImage = {},
                    onAddOrReplaceRule = {},
                    onRemoveRuleOfKind = {},
                    onSearchChange = {},
                )
            }
        }
    }

    @Test
    fun rendersTopSections() {
        setContent()
        composeTestRule.onNodeWithText(res.getString(R.string.apps_folder_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.apps_rule)).assertIsDisplayed()
    }

    @Test
    fun backAffordanceInvokesOnBack() {
        var backed = false
        setContent(onBack = { backed = true })
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.apps_back))
            .performClick()
        assertTrue(backed)
    }
}
