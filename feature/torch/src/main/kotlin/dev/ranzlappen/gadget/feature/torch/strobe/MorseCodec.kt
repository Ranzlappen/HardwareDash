package dev.ranzlappen.gadget.feature.torch.strobe

/**
 * International (ITU) Morse-code encoder for arbitrary text.
 *
 * [toTimeline] turns a string into an ordered list of
 * `(torchOn, durationMs)` steps suitable for the [StrobeService] loop,
 * using standard Morse timing relative to a dot [unit]:
 *  - dot = 1u on, dash = 3u on
 *  - gap between symbols in a letter = 1u off
 *  - gap between letters = 3u off
 *  - gap between words = 7u off
 *
 * Unknown / unsupported characters are skipped. The timeline ends with a
 * word gap so a repeating caller reads cleanly between loops.
 */
object MorseCodec {

    private val CODE: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-",
        '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
        '@' to ".--.-.",
    )

    /** True if [text] contains at least one encodable character. */
    fun isEncodable(text: String): Boolean =
        text.uppercase().any { it == ' ' || CODE.containsKey(it) }

    fun toTimeline(text: String, unit: Long): List<Pair<Boolean, Long>> {
        val dot = unit
        val dash = unit * 3
        val symbolGap = unit
        val letterGap = unit * 3
        val wordGap = unit * 7

        val steps = mutableListOf<Pair<Boolean, Long>>()
        var emittedLetter = false
        text.uppercase().forEach { ch ->
            if (ch == ' ') {
                if (emittedLetter) {
                    trimTrailingGap(steps)
                    steps.add(false to wordGap)
                }
                emittedLetter = false
                return@forEach
            }
            val code = CODE[ch] ?: return@forEach
            if (emittedLetter) steps.add(false to letterGap)
            code.forEachIndexed { sIndex, symbol ->
                steps.add(true to if (symbol == '-') dash else dot)
                if (sIndex != code.lastIndex) steps.add(false to symbolGap)
            }
            emittedLetter = true
        }
        trimTrailingGap(steps)
        if (steps.isNotEmpty()) steps.add(false to wordGap)
        return steps
    }

    /** Drop a trailing OFF step so we don't double up gaps. */
    private fun trimTrailingGap(steps: MutableList<Pair<Boolean, Long>>) {
        while (steps.isNotEmpty() && !steps.last().first) steps.removeAt(steps.lastIndex)
    }
}
