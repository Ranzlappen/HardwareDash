package dev.ranzlappen.gadget.feature.torch.strobe

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MorseCodec.toTimeline].
 *
 * The full SOS pattern is the canonical regression check: every torch widget
 * that defaults to Morse plays it, so any drift in timing or step ordering
 * would surface there first.
 */
class MorseCodecTest {

    @Test
    fun `empty text yields empty timeline`() {
        assertEquals(emptyList(), MorseCodec.toTimeline("", unit = 100L))
    }

    @Test
    fun `unencodable text yields empty timeline`() {
        // No letter the codec recognises → no steps emitted.
        assertEquals(emptyList(), MorseCodec.toTimeline("©®", unit = 100L))
    }

    @Test
    fun `single letter S produces three ON dots with two intra-letter gaps`() {
        // S = ... → on,off,on,off,on then a final word-gap.
        val timeline = MorseCodec.toTimeline("S", unit = 100L)
        // 3 ON + 2 intra-letter OFF + 1 trailing word-gap = 6.
        assertEquals(6, timeline.size)
        assertEquals(true to 100L, timeline[0])
        assertEquals(false to 100L, timeline[1])
        assertEquals(true to 100L, timeline[2])
        assertEquals(false to 100L, timeline[3])
        assertEquals(true to 100L, timeline[4])
        // Trailing word-gap so a repeating caller reads cleanly between loops.
        assertEquals(false to 700L, timeline[5])
    }

    @Test
    fun `SOS timeline shape matches the canonical pattern`() {
        val unit = 100L
        val timeline = MorseCodec.toTimeline("SOS", unit)

        // Symbol counts: S=3 dots + 2 gaps, O=3 dashes + 2 gaps, S=3 dots + 2 gaps.
        // Letter gaps between letters: 2 (after first S, after O).
        // Plus the trailing word-gap.
        // Steps: (5+5+5) symbols/intra + 2 letter-gaps + 1 word-gap = 18.
        assertEquals(18, timeline.size)

        // Nine ON steps (3 per letter × 3 letters).
        val onSteps = timeline.count { it.first }
        assertEquals(9, onSteps)

        // Every ON step in S is a dot (1u); every ON step in O is a dash (3u).
        // Pull out just the ON durations in order.
        val onDurations = timeline.filter { it.first }.map { it.second }
        assertEquals(listOf(100L, 100L, 100L, 300L, 300L, 300L, 100L, 100L, 100L), onDurations)

        // Spot-check key gaps:
        //  - the letter gap immediately after the first S (3u),
        //  - the trailing word gap (7u).
        assertTrue(timeline.any { !it.first && it.second == 300L })
        assertEquals(false to 700L, timeline.last())
    }

    @Test
    fun `isEncodable returns true for whitespace and known letters`() {
        assertTrue(MorseCodec.isEncodable("SOS"))
        assertTrue(MorseCodec.isEncodable("hi there"))
        assertTrue(MorseCodec.isEncodable(" "))
    }

    @Test
    fun `isEncodable returns false for purely-unknown input`() {
        // Only characters the codec doesn't know — no symbols, no whitespace.
        assertEquals(false, MorseCodec.isEncodable("©®"))
    }
}
