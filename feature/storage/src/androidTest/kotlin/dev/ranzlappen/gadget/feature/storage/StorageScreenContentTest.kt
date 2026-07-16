package dev.ranzlappen.gadget.feature.storage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [StorageScreenContent].
 *
 * Exercises the stateless screen with a representative volume list (and the
 * empty/loading case) and asserts the rendered screen title + card headers.
 * Mirrors the torch reference pattern (`createComposeRule` + [GadgetTestTheme]).
 *
 * Tests run via `:feature:storage:connectedDebugAndroidTest`. CI emulator
 * workflow tracked at https://github.com/Ranzlappen/HardwareDash/issues/92.
 */
@RunWith(AndroidJUnit4::class)
class StorageScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(volumes: List<StorageVolumeInfo>) {
        composeTestRule.setContent {
            GadgetTestTheme {
                StorageScreenContent(
                    volumes = volumes,
                    moduleInfo = null,
                )
            }
        }
    }

    @Test
    fun rendersScreenTitleAndInternalVolumeCard() {
        setContent(
            listOf(
                StorageVolumeInfo(
                    label = "Internal shared storage",
                    totalBytes = 128L * 1024 * 1024 * 1024,
                    usedBytes = 87L * 1024 * 1024 * 1024,
                    freeBytes = 41L * 1024 * 1024 * 1024,
                    isRemovable = false,
                ),
            ),
        )
        composeTestRule.onNodeWithText(res.getString(R.string.storage_screen_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.storage_card_title_internal))
            .assertIsDisplayed()
    }

    @Test
    fun emptyVolumesShowsLoadingCard() {
        setContent(emptyList())
        composeTestRule.onNodeWithText(res.getString(R.string.storage_screen_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(res.getString(R.string.storage_reading))
            .assertIsDisplayed()
    }
}
