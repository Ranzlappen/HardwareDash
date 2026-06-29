package dev.ranzlappen.gadget.feature.flipper.rpc

/**
 * Storage-namespace RPC commands: mkdir, write (chunked), delete.
 *
 * Paths are absolute Flipper VFS paths, e.g. `/ext/subghz/test.sub`.
 */
class StorageCommands(private val client: FlipperRpcClient) {

    /** mkdir on the Flipper SD card. Tolerates "already exists". */
    suspend fun mkdir(path: String) {
        val body = pbMessage {
            writeString(1, path)
        }
        client.call(PbMain.C_STORAGE_MKDIR_REQUEST, body)
    }

    suspend fun delete(path: String, recursive: Boolean = false) {
        val body = pbMessage {
            writeString(1, path)
            writeBool(2, recursive)
        }
        client.call(PbMain.C_STORAGE_DELETE_REQUEST, body)
    }

    /**
     * Write a file by streaming WriteRequest frames. Each frame's content sub-
     * message has shape: `{ string path = 1; File file = 2 }`, where File is
     * `{ bytes data = 4 }`. The last frame sets `has_next = false`.
     *
     * Chunk size is bounded to keep BLE friendly (default 512).
     */
    suspend fun write(path: String, data: ByteArray, chunkSize: Int = 512) {
        val chunks = if (data.isEmpty()) listOf(ByteArray(0)) else data.toList().chunked(chunkSize) { it.toByteArray() }
        val bodies = chunks.asSequence().map { chunk ->
            pbMessage {
                writeString(1, path)
                val fileBody = pbMessage {
                    writeBytesField(4, chunk)
                }
                writeMessageField(2, fileBody)
            }
        }
        client.callStreaming(PbMain.C_STORAGE_WRITE_REQUEST, bodies)
    }
}
