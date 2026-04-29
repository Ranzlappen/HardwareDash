package com.gadget.ir

object IrCodecs {

    data class EncodedIr(val carrierHz: Int, val pattern: IntArray)

    sealed class Result {
        data class Ok(val encoded: EncodedIr) : Result()
        data class Error(val message: String) : Result()
    }

    fun encode(protocol: String, payload: String, carrierHz: Int, repeats: Int): Result {
        return try {
            val once = when (protocol.uppercase()) {
                "NEC" -> encodeNec(payload)
                "PRONTO" -> return encodePronto(payload, repeats)
                "RAW" -> encodeRaw(payload)
                else -> return Result.Error("Unsupported protocol: $protocol")
            }
            val combined = repeatPattern(once, repeats.coerceAtLeast(1), gapUs = 40_000)
            Result.Ok(EncodedIr(carrierHz, combined))
        } catch (e: IllegalArgumentException) {
            Result.Error(e.message ?: "Invalid input")
        } catch (e: Exception) {
            Result.Error("Encode failed: ${e.message}")
        }
    }

    private fun encodeNec(payload: String): IntArray {
        val hex = payload.trim().removePrefix("0x").removePrefix("0X")
        require(hex.matches(Regex("[0-9A-Fa-f]+"))) { "NEC payload must be hex" }
        require(hex.length in 1..8) { "NEC code is 1-32 bits (max 8 hex chars)" }
        val bits = hex.length * 4
        val value = hex.toLong(16)

        val out = ArrayList<Int>(4 + bits * 2 + 2)
        out += 9000   // leader mark
        out += 4500   // leader space
        for (i in bits - 1 downTo 0) {
            val bit = ((value shr i) and 1L).toInt()
            out += 560
            out += if (bit == 1) 1690 else 560
        }
        out += 560    // stop mark
        return out.toIntArray()
    }

    private fun encodeRaw(payload: String): IntArray {
        val parts = payload.split(',', ' ', '\n', '\t', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        require(parts.isNotEmpty()) { "Raw payload is empty" }
        require(parts.size % 2 == 0) { "Raw payload must have an even number of values (mark/space pairs)" }
        return parts.map {
            val n = it.toInt()
            require(n > 0) { "Raw values must be positive microseconds" }
            n
        }.toIntArray()
    }

    private fun encodePronto(payload: String, repeats: Int): Result {
        val words = payload.split(' ', '\n', '\t', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (words.size < 4) return Result.Error("Pronto needs at least header + counts")
        val header = words[0].toInt(16)
        if (header != 0x0000) return Result.Error("Only learned (0000) Pronto codes supported")
        val freqCode = words[1].toInt(16)
        if (freqCode <= 0) return Result.Error("Invalid Pronto carrier code")
        val periodUs = freqCode * 0.241246
        val carrierHz = (1_000_000.0 / periodUs).toInt()
        val onceCount = words[2].toInt(16)
        val repeatCount = words[3].toInt(16)
        val pairs = words.drop(4).map { it.toInt(16) }
        val expected = (onceCount + repeatCount) * 2
        if (pairs.size < expected) return Result.Error("Pronto burst pairs truncated")

        fun pairsToUs(start: Int, count: Int): IntArray {
            val out = IntArray(count * 2)
            for (i in 0 until count * 2) {
                out[i] = (pairs[start + i] * periodUs).toInt().coerceAtLeast(1)
            }
            return out
        }

        val once = pairsToUs(0, onceCount)
        val repeat = if (repeatCount > 0) pairsToUs(onceCount * 2, repeatCount) else IntArray(0)

        val total = ArrayList<Int>(once.size + repeat.size * repeats)
        if (onceCount > 0) total += once.toList()
        val replays = if (onceCount > 0) (repeats - 1).coerceAtLeast(0) else repeats.coerceAtLeast(1)
        repeat(replays) { total += repeat.toList() }
        if (total.isEmpty()) return Result.Error("Pronto produced empty pattern")
        if (total.size % 2 != 0) total += 1  // ensure trailing space slot
        return Result.Ok(EncodedIr(carrierHz, total.toIntArray()))
    }

    private fun repeatPattern(once: IntArray, times: Int, gapUs: Int): IntArray {
        if (times <= 1) return once
        val out = ArrayList<Int>(once.size * times + (times - 1) * 2)
        repeat(times) { idx ->
            out += once.toList()
            if (idx < times - 1) {
                if (out.size % 2 == 0) out += 1   // pad so next slot is the space-gap
                out += gapUs
            }
        }
        return out.toIntArray()
    }
}
