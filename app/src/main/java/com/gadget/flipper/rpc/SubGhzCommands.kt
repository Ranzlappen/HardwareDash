package com.gadget.flipper.rpc

/**
 * Sub-GHz transmission via the Flipper "subghz" application.
 *
 * Flipper RPC has no first-class Sub-GHz transmit command — instead you start
 * the subghz app via `App.StartRequest` with `tx <path>` args, which streams
 * the file once and then exits. The app must currently not be running.
 */
class SubGhzCommands(
    private val client: FlipperRpcClient,
    private val storage: StorageCommands,
) {

    /**
     * Upload [subFileContent] (the textual `.sub` body) to a temp path on the
     * SD card and tell the Flipper to transmit it.
     */
    suspend fun transmitSubFile(subFileContent: String, remoteName: String = "hardwaredash_tx.sub") {
        val path = "/ext/subghz/$remoteName"
        runCatching { storage.mkdir("/ext/subghz") }
        storage.write(path, subFileContent.toByteArray(Charsets.UTF_8))
        startApp(name = "Sub-GHz", args = "tx $path")
    }

    /** Stop any currently-running app on the Flipper. */
    suspend fun stop() {
        client.call(PbMain.C_APP_EXIT_REQUEST, ByteArray(0))
    }

    private suspend fun startApp(name: String, args: String) {
        val body = pbMessage {
            writeString(1, name)
            writeString(2, args)
        }
        client.call(PbMain.C_APP_START_REQUEST, body)
    }
}
