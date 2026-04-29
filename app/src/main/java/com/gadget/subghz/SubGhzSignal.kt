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
}
