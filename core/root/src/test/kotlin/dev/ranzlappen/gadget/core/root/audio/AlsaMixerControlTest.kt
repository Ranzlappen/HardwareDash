package dev.ranzlappen.gadget.core.root.audio

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.core.RootShellResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AlsaMixerControl] — the `tinymix` wrapper that drives
 * MicGainBoost. Exercised entirely against a [FakeRootShell] since the real
 * dependency ([RootShell]) is a single-method suspend interface with no
 * Android framework ties.
 */
class AlsaMixerControlTest {

    private val shell = FakeRootShell()
    private val control = AlsaMixerControl(shell)

    @Test
    fun `snapshot returns null when no candidate control is present`() = runTest {
        shell.listOutput = listOf("Some Unrelated Control")

        val snapshot = control.snapshot(listOf("MIC1 Gain", "ADC Volume"))

        assertNull(snapshot)
        assertTrue(shell.executedCommands.none { it.startsWith("tinymix get") })
    }

    @Test
    fun `snapshot returns null when the tinymix listing itself fails`() = runTest {
        shell.listSucceeds = false
        shell.listOutput = listOf("MIC1 Gain")

        val snapshot = control.snapshot(listOf("MIC1 Gain"))

        assertNull(snapshot)
        assertTrue(shell.executedCommands.none { it.startsWith("tinymix get") })
    }

    @Test
    fun `snapshot captures trimmed values only for available controls`() = runTest {
        shell.listOutput = listOf("MIC1 Gain", "ADC Volume")
        shell.getResponses["MIC1 Gain"] = "  5  "
        shell.getResponses["ADC Volume"] = "3"

        val snapshot = control.snapshot(listOf("MIC1 Gain", "ADC Volume", "Nonexistent Control"))

        assertEquals(mapOf("MIC1 Gain" to "5", "ADC Volume" to "3"), snapshot?.controls)
        assertTrue(shell.executedCommands.none { it.contains("Nonexistent Control") })
    }

    @Test
    fun `snapshot skips controls whose get fails and keeps the rest`() = runTest {
        shell.listOutput = listOf("MIC1 Gain", "ADC Volume")
        shell.failingGets += "MIC1 Gain"
        shell.getResponses["ADC Volume"] = "3"

        val snapshot = control.snapshot(listOf("MIC1 Gain", "ADC Volume"))

        assertEquals(mapOf("ADC Volume" to "3"), snapshot?.controls)
    }

    @Test
    fun `snapshot returns null when every available control fails to read`() = runTest {
        shell.listOutput = listOf("MIC1 Gain")
        shell.failingGets += "MIC1 Gain"

        val snapshot = control.snapshot(listOf("MIC1 Gain"))

        assertNull(snapshot)
    }

    @Test
    fun `setGainDb clamps a boost above the hard ceiling`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "10"))

        control.setGainDb(snapshot, boostDb = 999)

        // MIC_GAIN_HARD_DB_CEILING = 30, MIC_GAIN_DB_PER_RAW_STEP = 1 -> target = 10 + 30
        assertTrue(shell.executedCommands.contains("tinymix set \"MIC1 Gain\" 40"))
    }

    @Test
    fun `setGainDb clamps a negative boost to zero`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "10"))

        control.setGainDb(snapshot, boostDb = -50)

        assertTrue(shell.executedCommands.contains("tinymix set \"MIC1 Gain\" 10"))
    }

    @Test
    fun `setGainDb treats a non-numeric baseline as zero`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "not-a-number"))

        control.setGainDb(snapshot, boostDb = 5)

        assertTrue(shell.executedCommands.contains("tinymix set \"MIC1 Gain\" 5"))
    }

    @Test
    fun `setGainDb returns true when at least one control write succeeds`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "0", "ADC Volume" to "0"))
        shell.failingSets += "MIC1 Gain"

        val result = control.setGainDb(snapshot, boostDb = 5)

        assertTrue(result)
    }

    @Test
    fun `setGainDb returns false when every control write fails`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "0", "ADC Volume" to "0"))
        shell.failingSets += "MIC1 Gain"
        shell.failingSets += "ADC Volume"

        val result = control.setGainDb(snapshot, boostDb = 5)

        assertFalse(result)
    }

    @Test
    fun `setControlValue reports the underlying shell result`() = runTest {
        shell.failingSets += "Capture Volume"

        assertFalse(control.setControlValue("Capture Volume", "7"))
        assertTrue(control.setControlValue("DMIC Gain", "7"))
    }

    @Test
    fun `restore writes back every original value unmodified`() = runTest {
        val snapshot = MixerSnapshot(mapOf("MIC1 Gain" to "7", "ADC Volume" to "3"))

        control.restore(snapshot)

        assertTrue(shell.executedCommands.contains("tinymix set \"MIC1 Gain\" \"7\""))
        assertTrue(shell.executedCommands.contains("tinymix set \"ADC Volume\" \"3\""))
    }

    @Test
    fun `gainControlNames filters the fixed candidate list by tinymix availability`() = runTest {
        // Candidate order per AlsaMixerControl.kt: MIC1 Gain, MIC2 Gain,
        // ADC Volume, Capture Volume, DMIC Gain. Matching is substring-based
        // against the joined `tinymix` listing, not exact-line matching.
        shell.listOutput = listOf("0 DMIC Gain (range -12 to 12)", "1 MIC1 Gain (range 0 to 30)")

        val names = control.gainControlNames()

        assertEquals(listOf("MIC1 Gain", "DMIC Gain"), names)
    }

    @Test
    fun `gainControlNames is empty when the tinymix listing fails`() = runTest {
        shell.listSucceeds = false

        assertTrue(control.gainControlNames().isEmpty())
    }

    private class FakeRootShell : RootShell {
        val executedCommands = mutableListOf<String>()
        var listOutput: List<String> = emptyList()
        var listSucceeds = true
        val getResponses = mutableMapOf<String, String>()
        val failingGets = mutableSetOf<String>()
        val failingSets = mutableSetOf<String>()

        override suspend fun exec(command: String, timeoutMillis: Long?): RootShellResult {
            executedCommands += command
            val quotedName = QUOTED_NAME.find(command)?.groupValues?.get(1)
            return when {
                command == "tinymix" -> if (listSucceeds) ok(listOutput) else fail()
                command.startsWith("tinymix get ") -> {
                    val name = quotedName.orEmpty()
                    if (name in failingGets) fail() else ok(listOf(getResponses[name] ?: "0"))
                }
                command.startsWith("tinymix set ") -> {
                    val name = quotedName.orEmpty()
                    if (name in failingSets) fail() else ok(emptyList())
                }
                else -> fail()
            }
        }

        override suspend fun exec(commands: List<String>, timeoutMillis: Long?): RootShellResult {
            executedCommands += commands
            return ok(emptyList())
        }

        private fun ok(stdout: List<String>) = RootShellResult(0, stdout, emptyList(), 0)
        private fun fail() = RootShellResult(1, emptyList(), emptyList(), 0)

        companion object {
            private val QUOTED_NAME = Regex("\"([^\"]+)\"")
        }
    }
}
