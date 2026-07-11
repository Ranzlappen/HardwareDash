package dev.ranzlappen.gadget.feature.radios.ir

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [IrCodecs.encode] — the NEC / Pronto (learned `0000` header
 * only) / RAW encoder migrated from `com.gadget.ir.IrCodecs`. Pure Kotlin, no
 * Android surface. Covers each protocol's happy path, its validation error
 * paths, the shared `repeatPattern` gap-insertion logic (NEC/RAW only —
 * Pronto builds its own repeat section), and the protocol-name dispatch
 * (unsupported protocol, case-insensitivity).
 */
class IrCodecsTest {

    private fun ok(result: IrCodecs.Result): IrCodecs.EncodedIr {
        assertTrue(result is IrCodecs.Result.Ok, "expected Ok but was $result")
        return (result as IrCodecs.Result.Ok).encoded
    }

    private fun error(result: IrCodecs.Result): String {
        assertTrue(result is IrCodecs.Result.Error, "expected Error but was $result")
        return (result as IrCodecs.Result.Error).message
    }

    // ---- protocol dispatch ----

    @Test
    fun `unsupported protocol name returns an error`() {
        val message = error(IrCodecs.encode("SIRC", "0x1234", 38_000, 1))

        assertEquals("Unsupported protocol: SIRC", message)
    }

    @Test
    fun `protocol name is matched case-insensitively`() {
        val lower = ok(IrCodecs.encode("nec", "0x00", 38_000, 1))
        val upper = ok(IrCodecs.encode("NEC", "0x00", 38_000, 1))

        assertEquals(lower.pattern.toList(), upper.pattern.toList())
    }

    // ---- NEC ----

    @Test
    fun `NEC encodes an 8-bit header followed by 2 pulses per bit and a trailing pulse`() {
        // 0xF0 = 1111_0000 (MSB first): four 1-bits then four 0-bits.
        val encoded = ok(IrCodecs.encode("NEC", "0xF0", 38_000, 1))

        val expected = intArrayOf(
            9000, 4500,
            560, 1690, 560, 1690, 560, 1690, 560, 1690,
            560, 560, 560, 560, 560, 560, 560, 560,
            560,
        )
        assertEquals(expected.toList(), encoded.pattern.toList())
        assertEquals(38_000, encoded.carrierHz)
    }

    @Test
    fun `NEC pattern length follows the 2 + bits x2 + 1 formula for a full 32-bit code`() {
        val encoded = ok(IrCodecs.encode("NEC", "0x12345678", 38_000, 1))

        assertEquals(2 + 32 * 2 + 1, encoded.pattern.size)
    }

    @Test
    fun `NEC accepts the shortest 1 hex-digit (4-bit) payload`() {
        val encoded = ok(IrCodecs.encode("NEC", "F", 38_000, 1))

        assertEquals(2 + 4 * 2 + 1, encoded.pattern.size)
    }

    @Test
    fun `NEC strips an 0x or 0X prefix before validating`() {
        val lower = ok(IrCodecs.encode("NEC", "0xAB", 38_000, 1))
        val upper = ok(IrCodecs.encode("NEC", "0XAB", 38_000, 1))
        val bare = ok(IrCodecs.encode("NEC", "AB", 38_000, 1))

        assertEquals(lower.pattern.toList(), upper.pattern.toList())
        assertEquals(lower.pattern.toList(), bare.pattern.toList())
    }

    @Test
    fun `NEC rejects a non-hex payload`() {
        val message = error(IrCodecs.encode("NEC", "0xZZ", 38_000, 1))

        assertEquals("NEC payload must be hex", message)
    }

    @Test
    fun `NEC rejects an empty payload after stripping the prefix`() {
        val message = error(IrCodecs.encode("NEC", "0x", 38_000, 1))

        assertEquals("NEC payload must be hex", message)
    }

    @Test
    fun `NEC rejects a payload longer than 8 hex chars`() {
        val message = error(IrCodecs.encode("NEC", "123456789", 38_000, 1))

        assertEquals("NEC code is 1–32 bits (max 8 hex chars)", message)
    }

    @Test
    fun `NEC carrierHz is passed through unchanged`() {
        val encoded = ok(IrCodecs.encode("NEC", "0x01", 56_000, 1))

        assertEquals(56_000, encoded.carrierHz)
    }

    // ---- RAW ----

    @Test
    fun `RAW parses comma-separated mark-space pairs`() {
        val encoded = ok(IrCodecs.encode("RAW", "9000,4500,560,560", 38_000, 1))

        assertEquals(listOf(9000, 4500, 560, 560), encoded.pattern.toList())
    }

    @Test
    fun `RAW accepts space, newline, tab and semicolon as separators`() {
        val encoded = ok(IrCodecs.encode("RAW", "100 200\n300\t400;500 600", 38_000, 1))

        assertEquals(listOf(100, 200, 300, 400, 500, 600), encoded.pattern.toList())
    }

    @Test
    fun `RAW rejects an empty payload`() {
        val message = error(IrCodecs.encode("RAW", "   ", 38_000, 1))

        assertEquals("Raw payload is empty", message)
    }

    @Test
    fun `RAW rejects an odd number of values`() {
        val message = error(IrCodecs.encode("RAW", "100,200,300", 38_000, 1))

        assertEquals("Raw payload must have an even number of values (mark/space pairs)", message)
    }

    @Test
    fun `RAW rejects a zero or negative value`() {
        val message = error(IrCodecs.encode("RAW", "100,0", 38_000, 1))

        assertEquals("Raw values must be positive microseconds", message)
    }

    @Test
    fun `RAW rejects a non-numeric value`() {
        val message = error(IrCodecs.encode("RAW", "100,abc", 38_000, 1))

        assertTrue(message.contains("abc"), "message should mention the bad token: $message")
    }

    // ---- repeatPattern (shared by NEC and RAW) ----

    @Test
    fun `repeats greater than 1 inserts a 40ms gap between copies, padded to keep parity when the copy length is even`() {
        // RAW mark-space pairs are always an even count (encodeRaw enforces
        // it), so `repeatPattern`'s "pad with 1us if the running length is
        // even" branch fires before every gap: once + [1, 40000] + once.
        val once = ok(IrCodecs.encode("RAW", "100,200", 38_000, 1)).pattern
        val twice = ok(IrCodecs.encode("RAW", "100,200", 38_000, 2)).pattern

        assertEquals(2, once.size)
        assertEquals(once.toList() + listOf(1, 40_000) + once.toList(), twice.toList())
    }

    @Test
    fun `repeats greater than 1 skips the padding when the copy length is already odd`() {
        // NEC's `2 + bits*2 + 1` length is always odd, so the running total
        // after one copy is already odd and the padding branch is skipped:
        // once + [40000] + once, with no extra padding value.
        val once = ok(IrCodecs.encode("NEC", "F", 38_000, 1)).pattern
        val twice = ok(IrCodecs.encode("NEC", "F", 38_000, 2)).pattern

        assertEquals(11, once.size)
        assertEquals(once.toList() + listOf(40_000) + once.toList(), twice.toList())
    }

    @Test
    fun `repeats of 1 or fewer produces a single copy with no gap`() {
        val once = ok(IrCodecs.encode("RAW", "100,200", 38_000, 1)).pattern
        val zero = ok(IrCodecs.encode("RAW", "100,200", 38_000, 0)).pattern
        val negative = ok(IrCodecs.encode("RAW", "100,200", 38_000, -3)).pattern

        assertEquals(once.toList(), zero.toList())
        assertEquals(once.toList(), negative.toList())
    }

    // ---- Pronto ----

    @Test
    fun `Pronto rejects fewer than 4 words`() {
        val message = error(IrCodecs.encode("PRONTO", "0000 006C 0002", 38_000, 1))

        assertEquals("Pronto needs at least header + counts", message)
    }

    @Test
    fun `Pronto rejects a non-learned header`() {
        val message = error(
            IrCodecs.encode("PRONTO", "0001 006C 0002 0000 0060 0030 0018 0018", 38_000, 1),
        )

        assertEquals("Only learned (0000) Pronto codes supported", message)
    }

    @Test
    fun `Pronto rejects a zero carrier frequency code`() {
        val message = error(
            IrCodecs.encode("PRONTO", "0000 0000 0002 0000 0060 0030 0018 0018", 38_000, 1),
        )

        assertEquals("Invalid Pronto carrier code", message)
    }

    @Test
    fun `Pronto rejects truncated burst pairs`() {
        // onceCount = 2 pairs (needs 4 words) but only 2 are supplied.
        val message = error(IrCodecs.encode("PRONTO", "0000 006C 0002 0000 0060 0030", 38_000, 1))

        assertEquals("Pronto burst pairs truncated", message)
    }

    @Test
    fun `Pronto rejects a header with zero once and repeat pairs`() {
        val message = error(IrCodecs.encode("PRONTO", "0000 006C 0000 0000", 38_000, 1))

        assertEquals("Pronto produced empty pattern", message)
    }

    @Test
    fun `Pronto decodes once-section pairs and derives the carrier from the frequency code`() {
        val encoded = ok(
            IrCodecs.encode("PRONTO", "0000 006C 0002 0000 0060 0030 0018 0018", 38_000, 1),
        )

        // freqCode 0x6C -> periodUs = 108 * 0.241246 ~= 26.0546us -> ~38380 Hz.
        assertEquals(38_380, encoded.carrierHz)
        assertEquals(listOf(2501, 1250, 625, 625), encoded.pattern.toList())
    }

    @Test
    fun `Pronto accepts comma, semicolon, newline and tab separated words`() {
        val spaceSeparated = ok(
            IrCodecs.encode("PRONTO", "0000 006C 0002 0000 0060 0030 0018 0018", 38_000, 1),
        )
        val mixedSeparated = ok(
            IrCodecs.encode("PRONTO", "0000,006C;0002\n0000\t0060,0030;0018,0018", 38_000, 1),
        )

        assertEquals(spaceSeparated.pattern.toList(), mixedSeparated.pattern.toList())
        assertEquals(spaceSeparated.carrierHz, mixedSeparated.carrierHz)
    }

    @Test
    fun `Pronto with a once section replays only the repeat section (repeats - 1) times`() {
        // once = 1 pair, repeat = 1 pair; repeats = 3 means the once section
        // plays once and the repeat section plays (3 - 1) = 2 times.
        val encoded = ok(
            IrCodecs.encode("PRONTO", "0000 006C 0001 0001 0064 0032 000A 000A", 38_000, 3),
        )

        assertEquals(listOf(2605, 1302, 260, 260, 260, 260), encoded.pattern.toList())
    }

    @Test
    fun `Pronto with no once section replays the repeat section 'repeats' times`() {
        // onceCount = 0, repeatCount = 1 pair; repeats = 2 means the repeat
        // section plays repeats.coerceAtLeast(1) = 2 times outright.
        val encoded = ok(
            IrCodecs.encode("PRONTO", "0000 006C 0000 0001 0014 0014", 38_000, 2),
        )

        assertEquals(listOf(521, 521, 521, 521), encoded.pattern.toList())
    }

    @Test
    fun `Pronto with no once section and repeats less than 1 still plays the repeat section once`() {
        val encoded = ok(
            IrCodecs.encode("PRONTO", "0000 006C 0000 0001 0014 0014", 38_000, 0),
        )

        assertEquals(listOf(521, 521), encoded.pattern.toList())
    }
}
