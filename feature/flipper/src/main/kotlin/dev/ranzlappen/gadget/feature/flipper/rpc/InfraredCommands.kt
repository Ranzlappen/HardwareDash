package dev.ranzlappen.gadget.feature.flipper.rpc

/**
 * IR transmission via the Flipper "infrared" application.
 *
 * Same pattern as Sub-GHz: write a `.ir` file then `App.StartRequest` with
 * `tx <path>`. We accept a Flipper-native `.ir` file body directly so callers
 * can hand-roll the format the device expects.
 */
class InfraredCommands(
    private val client: FlipperRpcClient,
    private val storage: StorageCommands,
) {

    suspend fun transmitIrFile(irFileContent: String, remoteName: String = "hardwaredash_tx.ir") {
        val path = "/ext/infrared/$remoteName"
        runCatching { storage.mkdir("/ext/infrared") }
        storage.write(path, irFileContent.toByteArray(Charsets.UTF_8))
        startApp(name = "Infrared", args = "tx $path")
    }

    /**
     * Build a minimal `.ir` Flipper file body for a parsed protocol button.
     * Format reference: https://docs.flipper.net/infrared/file-format
     */
    fun buildRawIrFile(
        name: String,
        frequencyHz: Int,
        dutyCycle: Double,
        timingsMicros: IntArray,
    ): String = buildString {
        append("Filetype: IR signals file\n")
        append("Version: 1\n")
        append("#\n")
        append("name: $name\n")
        append("type: raw\n")
        append("frequency: $frequencyHz\n")
        append("duty_cycle: ${"%.2f".format(dutyCycle)}\n")
        append("data: ${timingsMicros.joinToString(" ")}\n")
    }

    fun buildParsedIrFile(
        name: String,
        protocol: String,
        addressHex: String,
        commandHex: String,
    ): String = buildString {
        append("Filetype: IR signals file\n")
        append("Version: 1\n")
        append("#\n")
        append("name: $name\n")
        append("type: parsed\n")
        append("protocol: $protocol\n")
        append("address: $addressHex\n")
        append("command: $commandHex\n")
    }

    private suspend fun startApp(name: String, args: String) {
        val body = pbMessage {
            writeString(1, name)
            writeString(2, args)
        }
        client.call(PbMain.C_APP_START_REQUEST, body)
    }
}
