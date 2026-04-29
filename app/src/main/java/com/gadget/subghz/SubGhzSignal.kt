package com.gadget.subghz

object SubGhzSignal {

    data class Parsed(
        val frequencyHz: Long,
        val preset: String,
        val protocol: String,
        val bitLength: Int,
        val keyHex: String,
        val rawData: String,
        val te: Int,
        val repeat: Int,
    )

    /**
     * Parse a Flipper Zero `.sub` file's text contents.
     * Tolerates missing optional fields and unknown lines.
     * Returns null if `Filetype:` and `Frequency:` aren't both present.
     */
    fun parseFlipperSub(text: String): Parsed? {
        val map = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1).trim()
                if (k.isNotEmpty()) map[k] = v
            }
        }
        if (!map.containsKey("Filetype")) return null
        val freq = map["Frequency"]?.toLongOrNull() ?: return null

        val preset = map["Preset"].orEmpty()
        return Parsed(
            frequencyHz = freq,
            preset = preset,
            protocol = map["Protocol"].orEmpty(),
            bitLength = map["Bit"]?.toIntOrNull() ?: 0,
            keyHex = map["Key"].orEmpty(),
            rawData = map["RAW_Data"].orEmpty(),
            te = map["TE"]?.toIntOrNull() ?: 0,
            repeat = map["Repeat"]?.toIntOrNull() ?: 0,
        )
    }

    /**
     * Map a Flipper preset constant to a short modulation label used in the UI.
     */
    fun modulationFromPreset(preset: String): String = when {
        preset.contains("Ook650", ignoreCase = true) -> "AM650"
        preset.contains("Ook270", ignoreCase = true) -> "AM270"
        preset.contains("2FSKDev238", ignoreCase = true) -> "FM238"
        preset.contains("2FSKDev476", ignoreCase = true) -> "FM476"
        preset.contains("Custom", ignoreCase = true) -> "Custom"
        preset.isBlank() -> ""
        else -> preset
    }

    /** Inverse of [modulationFromPreset]: UI label → Flipper preset constant. */
    fun presetFromModulation(modulation: String): String = when (modulation) {
        "AM650" -> "FuriHalSubGhzPresetOok650Async"
        "AM270" -> "FuriHalSubGhzPresetOok270Async"
        "FM238" -> "FuriHalSubGhzPreset2FSKDev238Async"
        "FM476" -> "FuriHalSubGhzPreset2FSKDev476Async"
        else -> modulation.ifBlank { "FuriHalSubGhzPresetOok650Async" }
    }
}

/**
 * Build a Flipper-format `.sub` text body from current Sub-GHz UI state.
 * Used to hand a transmittable file to a connected Flipper Zero.
 */
fun buildFlipperSubFile(
    frequencyHz: Long,
    preset: String,
    protocol: String,
    bitLength: Int,
    keyHex: String,
    rawData: String,
    te: Int,
): String = buildString {
    append("Filetype: Flipper SubGhz Key File\n")
    append("Version: 1\n")
    append("Frequency: $frequencyHz\n")
    append("Preset: ${SubGhzSignal.presetFromModulation(preset)}\n")
    val proto = protocol.ifBlank { "RAW" }
    append("Protocol: $proto\n")
    if (bitLength > 0) append("Bit: $bitLength\n")
    if (keyHex.isNotBlank()) append("Key: $keyHex\n")
    if (te > 0) append("TE: $te\n")
    if (rawData.isNotBlank()) append("RAW_Data: $rawData\n")
}
