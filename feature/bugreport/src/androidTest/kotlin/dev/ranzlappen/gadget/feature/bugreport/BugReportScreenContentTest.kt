package dev.ranzlappen.gadget.feature.bugreport

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [BugReportScreenContent] — the actionable
 * permission surface (Hilt-free; callbacks captured locally).
 */
@RunWith(AndroidJUnit4::class)
class BugReportScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val state = BugReportState(
        permissions = listOf(
            PermissionEntry(R.string.bugreport_perm_camera, "android.permission.CAMERA", true),
            PermissionEntry(R.string.bugreport_perm_microphone, "android.permission.RECORD_AUDIO", false),
        ),
    )

    @Test
    fun summary_countsGrantedOverTotal() {
        composeTestRule.setContent {
            GadgetTestTheme { BugReportScreenContent(state = state, moduleInfo = null) }
        }
        composeTestRule.onNodeWithText("1 of 2 granted").assertIsDisplayed()
    }

    @Test
    fun deniedPermission_grantButtonRequestsThatPermission() {
        var requested: String? = null
        composeTestRule.setContent {
            GadgetTestTheme {
                BugReportScreenContent(
                    state = state,
                    moduleInfo = null,
                    onRequestPermission = { requested = it },
                )
            }
        }
        composeTestRule.onNodeWithText("Grant").performClick()
        assertEquals("android.permission.RECORD_AUDIO", requested)
    }

    @Test
    fun missingPermissions_showOpenAppSettings() {
        var opened = false
        composeTestRule.setContent {
            GadgetTestTheme {
                BugReportScreenContent(
                    state = state,
                    moduleInfo = null,
                    onOpenAppSettings = { opened = true },
                )
            }
        }
        composeTestRule.onNodeWithText("Open App Settings").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun allGranted_hidesOpenAppSettings() {
        val allGranted = BugReportState(
            permissions = listOf(
                PermissionEntry(R.string.bugreport_perm_camera, "android.permission.CAMERA", true),
            ),
        )
        composeTestRule.setContent {
            GadgetTestTheme { BugReportScreenContent(state = allGranted, moduleInfo = null) }
        }
        composeTestRule.onNodeWithText("Open App Settings").assertDoesNotExist()
    }
}
