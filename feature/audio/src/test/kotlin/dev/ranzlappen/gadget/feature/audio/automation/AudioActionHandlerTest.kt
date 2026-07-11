package dev.ranzlappen.gadget.feature.audio.automation

import android.content.Context
import android.content.Intent
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.audio.AudioRecordService
import io.mockk.EqMatcher
import io.mockk.anyConstructed
import io.mockk.constructedWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AudioActionHandler]. Both actions build a real [Intent]
 * and hand it to `Context.startForegroundService`/`Context.startService` —
 * `Build.VERSION.SDK_INT` resolves to the stub jar's default of `0` on a
 * plain JVM unit test (no Robolectric shadow), so the `>= O` branch never
 * trips and `startService` is always the method actually invoked here,
 * mirroring `SettingsViewModelTest`'s `setFloatingTorchButtonEnabled`
 * coverage. `Intent` construction is intercepted via `mockkConstructor`
 * (plain `io.mockk:mockk`, no `mockk-android` needed — same technique as
 * `SettingsViewModelTest`).
 */
class AudioActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val handler = AudioActionHandler(context)

    @Before
    fun setUp() {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(AudioActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `declares start and stop recording actions that do not require root`() {
        assertEquals(
            setOf(AudioActionHandler.ACTION_START_RECORDING, AudioActionHandler.ACTION_STOP_RECORDING),
            handler.actions.map { it.key }.toSet(),
        )
        // No privileged shell involved — both actions just start the (unprivileged) service.
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `audio_start_recording builds an Intent carrying the start action and returns Success`() = runTest {
        val result = handler.dispatch(AudioActionHandler.ACTION_START_RECORDING, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify {
            constructedWith<Intent>(EqMatcher(AudioRecordService.ACTION_START_RECORD)).setPackage(any())
        }
        verify { context.startService(any()) }
    }

    @Test
    fun `audio_start_recording swallows an exception and returns a Failure carrying its message`() = runTest {
        every { context.startService(any()) } throws RuntimeException("service unavailable")

        val result = handler.dispatch(AudioActionHandler.ACTION_START_RECORDING, emptyMap())

        assertEquals(ActionResult.Failure("service unavailable"), result)
    }

    @Test
    fun `audio_start_recording falls back to a default message when the exception carries none`() = runTest {
        every { context.startService(any()) } throws RuntimeException()

        val result = handler.dispatch(AudioActionHandler.ACTION_START_RECORDING, emptyMap())

        assertEquals(ActionResult.Failure("Failed to start recording"), result)
    }

    @Test
    fun `audio_stop_recording builds an Intent carrying the stop action and returns Success`() = runTest {
        val result = handler.dispatch(AudioActionHandler.ACTION_STOP_RECORDING, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify {
            constructedWith<Intent>(EqMatcher(AudioRecordService.ACTION_STOP_RECORD)).setPackage(any())
        }
        verify { context.startService(any()) }
    }
}
