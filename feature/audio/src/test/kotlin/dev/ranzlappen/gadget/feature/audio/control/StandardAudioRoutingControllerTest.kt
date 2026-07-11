package dev.ranzlappen.gadget.feature.audio.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Contract-pinning tests for [StandardAudioRoutingController] — the
 * standard-flavor implementation of [AudioRoutingController]. Every
 * privileged method must return [AudioRoutingControllerResult.Unsupported]
 * (there is no privileged shell in this APK to run `cmd audio ...`); the two
 * revert paths report a no-op-but-successful
 * [AudioRoutingControllerResult.ResetCompleted] instead, since "nothing to
 * revert" isn't a failure — pinning both shapes guards against either one
 * silently regressing to the other.
 */
class StandardAudioRoutingControllerTest {

    private val controller = StandardAudioRoutingController()

    @Test
    fun `bypassStreamVolumeCap is unsupported`() = runTest {
        val result = controller.bypassStreamVolumeCap(
            StreamVolumeBypassConfig(stream = AudioStreamType.MUSIC, percent = 150, activeWindowMillis = 60_000),
        )

        assertEquals(AudioRoutingControllerResult.Unsupported, result)
    }

    @Test
    fun `forceRouting is unsupported`() = runTest {
        val result = controller.forceRouting(ForceRoutingConfig(target = AudioRoutingTarget.SPEAKER))

        assertEquals(AudioRoutingControllerResult.Unsupported, result)
    }

    @Test
    fun `muteAllStreams is unsupported`() = runTest {
        val result = controller.muteAllStreams(MuteAllStreamsConfig(durationMillis = 5_000))

        assertEquals(AudioRoutingControllerResult.Unsupported, result)
    }

    @Test
    fun `dumpAudioPolicy is unsupported`() = runTest {
        assertEquals(AudioRoutingControllerResult.Unsupported, controller.dumpAudioPolicy())
    }

    @Test
    fun `resetAllAudioRoutingMutations reports a no-op completion`() = runTest {
        assertEquals(
            AudioRoutingControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.resetAllAudioRoutingMutations(),
        )
    }

    @Test
    fun `revertOnScreenExit reports a no-op completion`() = runTest {
        assertEquals(
            AudioRoutingControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.revertOnScreenExit(),
        )
    }
}
