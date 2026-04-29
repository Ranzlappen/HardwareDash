package com.gadget.flipper.rpc

import java.io.ByteArrayOutputStream

/**
 * Minimal protobuf wire-format helpers — just enough to encode and decode the
 * subset of Flipper RPC messages we need.
 *
 * Wire types we use:
 *   0 = varint  (uint32, bool, enum)
 *   2 = length-delimited (string, bytes, embedded message)
 */

internal const val PB_WIRE_VARINT = 0
internal const val PB_WIRE_LEN = 2

internal fun pbTag(field: Int, wire: Int): Int = (field shl 3) or wire

internal fun ByteArrayOutputStream.writeVarint(value: Long) {
    var v = value
    while (true) {
        if ((v and 0x7FL.inv()) == 0L) {
            write(v.toInt())
            return
        } else {
            write(((v and 0x7FL) or 0x80L).toInt())
            v = v ushr 7
        }
    }
}

internal fun ByteArrayOutputStream.writeVarint(value: Int) {
    writeVarint(value.toLong() and 0xFFFFFFFFL)
}

internal fun ByteArrayOutputStream.writeTag(field: Int, wire: Int) {
    writeVarint(pbTag(field, wire))
}

internal fun ByteArrayOutputStream.writeUint32(field: Int, value: Int) {
    if (value == 0) return
    writeTag(field, PB_WIRE_VARINT)
    writeVarint(value)
}

internal fun ByteArrayOutputStream.writeBool(field: Int, value: Boolean) {
    if (!value) return
    writeTag(field, PB_WIRE_VARINT)
    writeVarint(if (value) 1 else 0)
}

internal fun ByteArrayOutputStream.writeString(field: Int, value: String) {
    if (value.isEmpty()) return
    writeBytesField(field, value.toByteArray(Charsets.UTF_8))
}

internal fun ByteArrayOutputStream.writeBytesField(field: Int, value: ByteArray) {
    if (value.isEmpty()) return
    writeTag(field, PB_WIRE_LEN)
    writeVarint(value.size)
    write(value)
}

internal fun ByteArrayOutputStream.writeMessageField(field: Int, body: ByteArray) {
    writeTag(field, PB_WIRE_LEN)
    writeVarint(body.size)
    write(body)
}

/** Build a sub-message body and return its bytes. */
internal inline fun pbMessage(build: ByteArrayOutputStream.() -> Unit): ByteArray {
    val baos = ByteArrayOutputStream()
    baos.build()
    return baos.toByteArray()
}

/**
 * Forward-only protobuf cursor over a byte buffer. Not a full implementation —
 * skips unknown fields, exposes typed readers for the field types we use.
 */
internal class PbReader(private val buf: ByteArray, private val end: Int = buf.size, private var pos: Int = 0) {

    fun hasMore(): Boolean = pos < end

    fun readTag(): Int = readVarint().toInt()

    fun fieldOf(tag: Int): Int = tag ushr 3
    fun wireOf(tag: Int): Int = tag and 0x7

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            check(pos < end) { "Truncated varint" }
            val b = buf[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
            check(shift < 64) { "Varint too long" }
        }
    }

    fun readUint32(): Int = readVarint().toInt()
    fun readBool(): Boolean = readVarint() != 0L

    fun readBytes(): ByteArray {
        val len = readVarint().toInt()
        check(pos + len <= end) { "Truncated bytes" }
        val out = buf.copyOfRange(pos, pos + len)
        pos += len
        return out
    }

    fun readString(): String = readBytes().toString(Charsets.UTF_8)

    fun skip(wire: Int) {
        when (wire) {
            PB_WIRE_VARINT -> readVarint()
            PB_WIRE_LEN -> {
                val len = readVarint().toInt()
                check(pos + len <= end) { "Truncated length-delimited skip" }
                pos += len
            }
            1 -> {
                check(pos + 8 <= end)
                pos += 8
            }
            5 -> {
                check(pos + 4 <= end)
                pos += 4
            }
            else -> error("Unknown wire type $wire")
        }
    }
}
