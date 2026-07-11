package dev.ranzlappen.gadget.feature.audio

import android.content.Context
import android.media.AudioRecord
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [AudioRecorder]'s deterministic guard clauses.
 *
 * `startRecording()`'s happy path launches an unbounded capture loop on a
 * hardcoded `Dispatchers.IO` `CoroutineScope` (no injected test dispatcher)
 * and `stopAndSave()`'s happy path writes through a real `ContentResolver` —
 * neither is safely exercisable from a plain JVM unit test without either
 * refactoring production code to accept an injected dispatcher (out of
 * scope for a test-only change; not a bug) or accepting a background thread
 * that outlives the test. This suite instead pins the early-return guards
 * that run synchronously, before the capture coroutine is ever launched,
 * plus `stopAndSave()`'s untouched-recorder short-circuit.
 *
 * `AudioRecord.getMinBufferSize` is a static native call and `AudioRecord`
 * itself is a hardware-backed class — both are intercepted via
 * `mockkStatic`/`mockkConstructor` (plain `io.mockk:mockk`, no
 * `mockk-android` needed) purely to reach the guard clause under test
 * without ever letting the loop start.
 */
class AudioRecorderTest {

    private val context = mockk<Context>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isRecording starts false`() {
        val recorder = AudioRecorder(context)

        assertEquals(false, recorder.isRecording.value)
    }

    @Test
    fun `stopAndSave returns null when nothing was ever recorded`() {
        val recorder = AudioRecorder(context)

        assertNull(recorder.stopAndSave())
        assertEquals(false, recorder.isRecording.value)
    }

    @Test
    fun `startRecording is a no-op when the device reports an invalid buffer size`() {
        mockkStatic(AudioRecord::class)
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns AudioRecord.ERROR_BAD_VALUE

        val recorder = AudioRecorder(context)
        recorder.startRecording()

        assertEquals(false, recorder.isRecording.value)
    }

    @Test
    fun `startRecording releases and stays idle when AudioRecord fails to initialize`() {
        mockkStatic(AudioRecord::class)
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns 4_096
        mockkConstructor(AudioRecord::class)
        every { anyConstructed<AudioRecord>().state } returns AudioRecord.STATE_UNINITIALIZED
        every { anyConstructed<AudioRecord>().release() } returns Unit

        val recorder = AudioRecorder(context)
        recorder.startRecording()

        assertEquals(false, recorder.isRecording.value)
        verify { anyConstructed<AudioRecord>().release() }
    }
}
