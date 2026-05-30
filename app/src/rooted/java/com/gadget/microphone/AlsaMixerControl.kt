package com.gadget.microphone

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val MIC_GAIN_HARD_DB_CEILING = 30
internal const val MIC_GAIN_HARD_CEILING_MILLIS = 60_000L
internal const val MIC_GAIN_DB_PER_RAW_STEP = 1
private const val TINYMIX_BIN = "tinymix"

private val MIC_GAIN_CONTROL_CANDIDATES = listOf(
    "MIC1 Gain",
    "MIC2 Gain",
    "ADC Volume",
    "Capture Volume",
    "DMIC Gain",
)

/**
 * Wraps `tinymix get/set` for mic-gain and effect toggles. Snapshots all
 * touched control values via [snapshot] and restores them via [restore]
 * — callers are expected to use these in a `try/finally` (or
 * `NonCancellable` finally) so a cancelled coroutine never leaves the mic
 * in a non-default state.
 *
 * On devices without `tinymix`, [snapshot] returns null and the caller
 * surfaces [MicrophoneControllerResult.Unsupported].
 */
@Singleton
class AlsaMixerControl @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(controls: List<String>): MixerSnapshot? {
        val available = pickAvailableControls(controls)
        if (available.isEmpty()) return null
        val captured = mutableMapOf<String, String>()
        for (name in available) {
            val read = shell.exec("$TINYMIX_BIN get \"$name\"")
            if (!read.isSuccess) continue
            captured[name] = read.stdout.lastOrNull()?.trim().orEmpty()
        }
        if (captured.isEmpty()) return null
        return MixerSnapshot(captured)
    }

    suspend fun setGainDb(snapshot: MixerSnapshot, boostDb: Int): Boolean {
        val clampedDb = boostDb.coerceIn(0, MIC_GAIN_HARD_DB_CEILING)
        val rawDelta = clampedDb / MIC_GAIN_DB_PER_RAW_STEP
        var anySuccess = false
        for ((name, original) in snapshot.controls) {
            val baseline = original.toIntOrNull() ?: 0
            val target = baseline + rawDelta
            val write = shell.exec("$TINYMIX_BIN set \"$name\" $target")
            if (write.isSuccess) anySuccess = true
        }
        return anySuccess
    }

    suspend fun setControlValue(name: String, value: String): Boolean =
        shell.exec("$TINYMIX_BIN set \"$name\" \"$value\"").isSuccess

    suspend fun restore(snapshot: MixerSnapshot) {
        for ((name, original) in snapshot.controls) {
            shell.exec("$TINYMIX_BIN set \"$name\" \"$original\"")
        }
    }

    suspend fun gainControlNames(): List<String> = pickAvailableControls(MIC_GAIN_CONTROL_CANDIDATES)

    private suspend fun pickAvailableControls(candidates: List<String>): List<String> {
        val list = shell.exec("$TINYMIX_BIN")
        if (!list.isSuccess) return emptyList()
        val all = list.stdout.joinToString("\n")
        return candidates.filter { all.contains(it) }
    }
}

data class MixerSnapshot(val controls: Map<String, String>)
