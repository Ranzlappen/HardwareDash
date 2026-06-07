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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Hilt-free [AppsScreenContent] — the stateless
 * folder grid. Renders curated folder lists and asserts the grid / empty
 * state and that tile/FAB interactions fire the right callbacks.
 *
 * Runs via `:feature:apps:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AppsScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun folder(id: Long, name: String) = Folder(
        id = id,
        name = name,
        baseColorArgb = 0xFF6750A4.toInt(),
        coverIcon = "auto",
        sortOrder = 0,
        locked = false,
        createdAt = 0L,
    )

    @Test
    fun rendersFolderNames() {
        composeTestRule.setContent {
            GadgetTestTheme {
                AppsScreenContent(
                    folders = listOf(folder(1L, "Games"), folder(2L, "Work")),
                    onOpenFolder = {},
                    onCreateFolder = {},
                    onDeleteFolder = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Games").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun showsEmptyStateWhenNoFolders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                AppsScreenContent(
                    folders = emptyList(),
                    onOpenFolder = {},
                    onCreateFolder = {},
                    onDeleteFolder = {},
                )
            }
        }
        composeTestRule.onNodeWithText(res.getString(R.string.apps_no_folders)).assertIsDisplayed()
    }

    @Test
    fun tappingFolderInvokesOpenCallback() {
        var opened = -1L
        composeTestRule.setContent {
            GadgetTestTheme {
                AppsScreenContent(
                    folders = listOf(folder(7L, "Media")),
                    onOpenFolder = { opened = it },
                    onCreateFolder = {},
                    onDeleteFolder = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Media").performClick()
        assertEquals(7L, opened)
    }

    @Test
    fun fabOpensCreateDialog() {
        composeTestRule.setContent {
            GadgetTestTheme {
                AppsScreenContent(
                    folders = emptyList(),
                    onOpenFolder = {},
                    onCreateFolder = {},
                    onDeleteFolder = {},
                )
            }
        }
        composeTestRule
            .onNodeWithContentDescription(res.getString(R.string.apps_create_folder))
            .performClick()
        // The create dialog hosts the folder-name field.
        composeTestRule.onNodeWithText(res.getString(R.string.apps_folder_name)).assertIsDisplayed()
    }
}
