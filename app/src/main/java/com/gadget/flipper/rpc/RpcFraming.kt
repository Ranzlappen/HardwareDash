package com.gadget.flipper.rpc

import java.io.ByteArrayOutputStream

/**
 * Flipper RPC frames every Main message with a varint length prefix.
 * This object frames outgoing bytes and re-assembles incoming ones from a
 * potentially-fragmented byte stream.
 */
internal object RpcFraming {

    /** Wrap a serialized PB_Main with its varint length prefix. */
    fun frame(message: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream(message.size + 5)
        baos.writeVarint(message.size)
        baos.write(message)
        return baos.toByteArray()
    }
}

/**
 * Stateful re-assembler. Feed bytes as they arrive from the link; pulls out
 * complete framed PB_Main messages.
 */
internal class FrameReader {

    private val buffer = ByteArrayOutputStream()

    fun feed(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): List<ByteArray> {
        buffer.write(bytes, offset, length)
        val out = mutableListOf<ByteArray>()
        while (true) {
            val raw = buffer.toByteArray()
            val parsed = tryConsume(raw) ?: break
            out += parsed.message
            buffer.reset()
            if (parsed.consumed < raw.size) {
                buffer.write(raw, parsed.consumed, raw.size - parsed.consumed)
            }
        }
        return out
    }

    private data class Parsed(val message: ByteArray, val consumed: Int)

    private fun tryConsume(raw: ByteArray): Parsed? {
        if (raw.isEmpty()) return null
        var len = 0L
        var shift = 0
        var i = 0
        while (true) {
            if (i >= raw.size) return null
            val b = raw[i].toInt() and 0xFF
            len = len or ((b and 0x7F).toLong() shl shift)
            i++
            if (b and 0x80 == 0) break
            shift += 7
            if (shift >= 64) error("Varint too long in frame")
        }
        val payloadLen = len.toInt()
        if (i + payloadLen > raw.size) return null
        val msg = raw.copyOfRange(i, i + payloadLen)
        return Parsed(msg, i + payloadLen)
    }
}
