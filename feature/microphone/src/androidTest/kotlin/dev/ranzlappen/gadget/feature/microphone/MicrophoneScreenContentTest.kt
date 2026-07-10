package dev.ranzlappen.gadget.feature.microphone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MicrophoneScreenContent].
 *
 * Exercises the stateless screen with curated [MicrophoneScreenState]
 * snapshots and asserts rendered title / rooted-vs-standard row state /
 * dispatched [MicrophoneUiEvent]s. Mirror of `VibrationScreenContentTest` /
 * `TorchScreenContentTest`. Runs via
 * `:feature:microphone:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class MicrophoneScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setContent(
        state: MicrophoneScreenState,
        events: MutableList<MicrophoneUiEvent> = mutableListOf(),
    ): MutableList<MicrophoneUiEvent> {
        composeTestRule.setContent {
            GadgetTestTheme {
                MicrophoneScreenContent(
                    state = state,
                    onEvent = { events += it },
                )
            }
        }
        return events
    }

    @Test
    fun rendersScreenTitle() {
        setContent(MicrophoneScreenState(isRootedFlavor = true))
        composeTestRule.onNodeWithText(res.getString(R.string.microphone_screen_title)).assertIsDisplayed()
    }

    @Test
    fun standardFlavorShowsRootRequiredBadge() {
        setContent(MicrophoneScreenState(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_root_required_badge))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun standardFlavorDisablesGainBoostRunButton() {
        setContent(MicrophoneScreenState(isRootedFlavor = false))
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_action_gain_boost))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun rootedFlavorEnablesGainBoostRunButtonAndDispatchesRun() {
        val events = setContent(MicrophoneScreenState(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_action_gain_boost))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf(MicrophoneUiEvent.GainBoostRun), events)
    }

    @Test
    fun customSampleRateRunOpensConfirmDialogInsteadOfDispatchingDirectly() {
        val events = setContent(MicrophoneScreenState(isRootedFlavor = true))
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_action_custom_rate))
            .performScrollTo()
            .performClick()
        assertEquals(listOf(MicrophoneUiEvent.CustomSampleRateRequest), events)
    }

    @Test
    fun customSampleRateConfirmDialogRendersRiskWarningAndConfirmDispatches() {
        val events = setContent(
            MicrophoneScreenState(isRootedFlavor = true, showCustomSampleRateConfirm = true),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_custom_rate_confirm_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_custom_rate_confirm_action))
            .performClick()
        assertTrue(events.contains(MicrophoneUiEvent.CustomSampleRateConfirm))
    }

    @Test
    fun systemAudioCaptureConfirmDialogRendersLegalWarningAndConfirmDispatches() {
        val events = setContent(
            MicrophoneScreenState(isRootedFlavor = true, showSystemAudioCaptureConfirm = true),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_system_audio_confirm_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_system_audio_confirm_action))
            .performClick()
        assertTrue(events.contains(MicrophoneUiEvent.SystemAudioCaptureConfirm))
    }

    @Test
    fun customSampleRateConfirmDialogCancelDismisses() {
        val events = setContent(
            MicrophoneScreenState(isRootedFlavor = true, showCustomSampleRateConfirm = true),
        )
        composeTestRule
            .onNodeWithText(res.getString(R.string.microphone_confirm_cancel))
            .performClick()
        assertTrue(events.contains(MicrophoneUiEvent.CustomSampleRateDismiss))
    }
}
