package dev.ranzlappen.gadget.feature.flipper.rpc

import java.io.ByteArrayOutputStream

/**
 * Subset of `PB_Main` from flipperzero-protobuf. Field numbers below mirror
 * https://github.com/flipperdevices/flipperzero-protobuf/blob/dev/flipper.proto
 * and are stable across recent firmware (0.80+).
 */
internal object PbMain {

    private const val F_COMMAND_ID = 1
    private const val F_COMMAND_STATUS = 2
    private const val F_HAS_NEXT = 3

    // Content oneof — only the fields we actually use:
    const val C_EMPTY = 4
    const val C_PING_REQUEST = 5
    const val C_PING_RESPONSE = 6
    const val C_STORAGE_INFO_REQUEST = 7
    const val C_STORAGE_INFO_RESPONSE = 8
    const val C_DEVICE_INFO_REQUEST = 12
    const val C_DEVICE_INFO_RESPONSE = 13
    const val C_STOP_SESSION = 19
    const val C_STORAGE_LIST_REQUEST = 23
    const val C_STORAGE_LIST_RESPONSE = 24
    const val C_STORAGE_WRITE_REQUEST = 27
    const val C_STORAGE_DELETE_REQUEST = 28
    const val C_STORAGE_MKDIR_REQUEST = 29
    const val C_APP_START_REQUEST = 38
    const val C_APP_LOAD_FILE_REQUEST = 57
    const val C_APP_BUTTON_PRESS_REQUEST = 58
    const val C_APP_BUTTON_RELEASE_REQUEST = 59
    const val C_APP_EXIT_REQUEST = 56
    const val C_POWER_INFO_REQUEST = 65
    const val C_POWER_INFO_RESPONSE = 66

    fun encode(commandId: Int, contentField: Int, contentBody: ByteArray, hasNext: Boolean = false): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.writeUint32(F_COMMAND_ID, commandId)
        baos.writeBool(F_HAS_NEXT, hasNext)
        baos.writeMessageField(contentField, contentBody)
        return baos.toByteArray()
    }

    fun encodeEmpty(commandId: Int, contentField: Int, hasNext: Boolean = false): ByteArray =
        encode(commandId, contentField, ByteArray(0), hasNext)

    data class Decoded(
        val commandId: Int,
        val commandStatus: Int,
        val hasNext: Boolean,
        val contentField: Int,
        val contentBody: ByteArray,
    )

    fun decode(bytes: ByteArray): Decoded {
        val r = PbReader(bytes)
        var commandId = 0
        var commandStatus = 0
        var hasNext = false
        var contentField = 0
        var contentBody = ByteArray(0)
        while (r.hasMore()) {
            val tag = r.readTag()
            val field = r.fieldOf(tag)
            val wire = r.wireOf(tag)
            when (field) {
                F_COMMAND_ID -> commandId = r.readUint32()
                F_COMMAND_STATUS -> commandStatus = r.readUint32()
                F_HAS_NEXT -> hasNext = r.readBool()
                else -> if (wire == PB_WIRE_LEN) {
                    contentField = field
                    contentBody = r.readBytes()
                } else {
                    r.skip(wire)
                }
            }
        }
        return Decoded(commandId, commandStatus, hasNext, contentField, contentBody)
    }
}

internal object PbStatus {
    const val OK = 0
    fun isOk(status: Int) = status == OK
}
