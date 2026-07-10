package dev.ranzlappen.gadget.feature.microphone.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.microphone.control.CustomRateConfig
import dev.ranzlappen.gadget.feature.microphone.control.DirectPcmConfig
import dev.ranzlappen.gadget.feature.microphone.control.GainBoostConfig
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneController
import dev.ranzlappen.gadget.feature.microphone.control.MicrophoneControllerResult
import dev.ranzlappen.gadget.feature.microphone.control.MultiMicConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MicrophoneActionHandler]. The context is relaxed-mocked
 * purely for `getString(...)` calls inside the `actions` list construction —
 * every dispatch branch reaches only the injected [MicrophoneController], so
 * these run as plain JVM unit tests (no Android runtime needed), mirroring
 * `CameraActionHandlerTest` / `VibrationActionHandlerTest`.
 */
class MicrophoneActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<MicrophoneController>(relaxed = true)
    private val handler = MicrophoneActionHandler(context, controller)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(MicrophoneActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `every action requires root`() {
        assertEquals(6, handler.actions.size)
        assertTrue(handler.actions.all { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `gain_boost builds config from params and maps Ok to Success`() = runTest {
        coEvery { controller.gainBoost(GainBoostConfig(boostDb = 15, durationMillis = 4_000)) } returns
            MicrophoneControllerResult.Ok
        val result = handler.dispatch(
            MicrophoneActionHandler.ACTION_GAIN_BOOST,
            mapOf(
                MicrophoneActionHandler.PARAM_BOOST_DB to "15",
                MicrophoneActionHandler.PARAM_DURATION_MS to "4000",
            ),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `gain_boost falls back to defaults when params are missing`() = runTest {
        coEvery { controller.gainBoost(GainBoostConfig(boostDb = 10, durationMillis = 5_000)) } returns
            MicrophoneControllerResult.Ok
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_GAIN_BOOST, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `direct_pcm builds config from params`() = runTest {
        val expected = DirectPcmConfig(sampleRate = 44_100, channelCount = 2, bitsPerSample = 24, durationMillis = 2_000)
        coEvery { controller.directPcm(expected) } returns MicrophoneControllerResult.Ok
        val result = handler.dispatch(
            MicrophoneActionHandler.ACTION_DIRECT_PCM,
            mapOf(
                MicrophoneActionHandler.PARAM_SAMPLE_RATE to "44100",
                MicrophoneActionHandler.PARAM_CHANNEL_COUNT to "2",
                MicrophoneActionHandler.PARAM_BITS_PER_SAMPLE to "24",
                MicrophoneActionHandler.PARAM_DURATION_MS to "2000",
            ),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `custom_sample_rate dispatches straight through without a confirm gate`() = runTest {
        val expected = CustomRateConfig(targetSampleRate = 192_000, durationMillis = 5_000)
        coEvery { controller.customSampleRate(expected) } returns MicrophoneControllerResult.Ok
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_CUSTOM_SAMPLE_RATE, emptyMap())
        assertEquals(ActionResult.Success, result)
        coVerify(exactly = 1) { controller.customSampleRate(expected) }
    }

    @Test
    fun `multi_mic_raw builds config from params`() = runTest {
        val expected = MultiMicConfig(durationMillis = 15_000, maxStreams = 2)
        coEvery { controller.multiMicRaw(expected) } returns MicrophoneControllerResult.Ok
        val result = handler.dispatch(
            MicrophoneActionHandler.ACTION_MULTI_MIC_RAW,
            mapOf(
                MicrophoneActionHandler.PARAM_DURATION_MS to "15000",
                MicrophoneActionHandler.PARAM_MAX_STREAMS to "2",
            ),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `disable_effects reaches the controller directly`() = runTest {
        coEvery { controller.disableEffects() } returns MicrophoneControllerResult.Ok
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_DISABLE_EFFECTS, emptyMap())
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `system_audio_capture passes the duration through`() = runTest {
        coEvery { controller.systemAudioCapture(120_000) } returns MicrophoneControllerResult.Ok
        val result = handler.dispatch(
            MicrophoneActionHandler.ACTION_SYSTEM_AUDIO_CAPTURE,
            mapOf(MicrophoneActionHandler.PARAM_DURATION_MS to "120000"),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `Unsupported maps to a Failure with reason`() = runTest {
        coEvery { controller.disableEffects() } returns MicrophoneControllerResult.Unsupported
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_DISABLE_EFFECTS, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `OptedOut maps to a Failure`() = runTest {
        coEvery { controller.disableEffects() } returns MicrophoneControllerResult.OptedOut
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_DISABLE_EFFECTS, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `RateLimited maps to a Failure`() = runTest {
        coEvery { controller.disableEffects() } returns MicrophoneControllerResult.RateLimited(2_000)
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_DISABLE_EFFECTS, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `HardwareError maps to a Failure carrying the message`() = runTest {
        coEvery { controller.disableEffects() } returns MicrophoneControllerResult.HardwareError("no control accepted the write")
        val result = handler.dispatch(MicrophoneActionHandler.ACTION_DISABLE_EFFECTS, emptyMap())
        assertTrue(result is ActionResult.Failure)
        assertEquals("no control accepted the write", (result as ActionResult.Failure).reason)
    }
}
