package com.hardwaredash.services

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * HCE service that emulates an NFC Type 4 Tag containing an NDEF message.
 * Responds to ISO 7816-4 SELECT and READ BINARY commands from NFC readers.
 */
class NfcEmulationService : HostApduService() {

    companion object {
        /** Set this before enabling emulation; null = no data to serve. */
        @Volatile
        var ndefMessageBytes: ByteArray? = null

        private val SELECT_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val UNKNOWN_CMD = byteArrayOf(0x6D.toByte(), 0x00)
        private val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // NDEF Tag Application AID
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // File IDs
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())
    }

    private var selectedFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return UNKNOWN_CMD

        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = commandApdu[2].toInt() and 0xFF
        val p2 = commandApdu[3].toInt() and 0xFF

        return when (ins) {
            0xA4 -> handleSelect(commandApdu)  // SELECT
            0xB0 -> handleReadBinary(p1, p2, commandApdu)  // READ BINARY
            else -> UNKNOWN_CMD
        }
    }

    private fun handleSelect(apdu: ByteArray): ByteArray {
        if (apdu.size < 5) return UNKNOWN_CMD
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return UNKNOWN_CMD
        val data = apdu.sliceArray(5 until 5 + lc)

        // SELECT by AID (application)
        val p1 = apdu[2].toInt() and 0xFF
        if (p1 == 0x04) {
            // Application select
            return if (data.contentEquals(NDEF_AID)) {
                selectedFile = null
                SELECT_OK
            } else {
                FILE_NOT_FOUND
            }
        }

        // SELECT by file ID
        if (data.size == 2) {
            return when {
                data.contentEquals(CC_FILE_ID) -> {
                    selectedFile = CC_FILE_ID
                    SELECT_OK
                }
                data.contentEquals(NDEF_FILE_ID) -> {
                    selectedFile = NDEF_FILE_ID
                    SELECT_OK
                }
                else -> FILE_NOT_FOUND
            }
        }

        return FILE_NOT_FOUND
    }

    private fun handleReadBinary(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        val offset = (p1 shl 8) or p2
        val le = if (apdu.size > 4) apdu[apdu.size - 1].toInt() and 0xFF else 0

        val fileData = when {
            selectedFile?.contentEquals(CC_FILE_ID) == true -> buildCcFile()
            selectedFile?.contentEquals(NDEF_FILE_ID) == true -> buildNdefFile()
            else -> return FILE_NOT_FOUND
        } ?: return FILE_NOT_FOUND

        val readLen = if (le == 0) fileData.size - offset else le.coerceAtMost(fileData.size - offset)
        if (offset >= fileData.size || readLen <= 0) return SELECT_OK

        return fileData.sliceArray(offset until offset + readLen) + SELECT_OK
    }

    /** Build the Capability Container (CC) file for Type 4 Tag. */
    private fun buildCcFile(): ByteArray {
        val ndefFile = buildNdefFile() ?: return byteArrayOf(
            0x00, 0x0F,             // CC length = 15
            0x20,                   // Mapping version 2.0
            0x00, 0x3B,            // Max R-APDU = 59
            0x00, 0x34,            // Max C-APDU = 52
            0x04, 0x06,            // NDEF TLV: type=4, length=6
            0xE1.toByte(), 0x04,   // NDEF file ID
            0x00, 0x00,            // Size = 0 (no data)
            0x00,                   // Read access: open
            0xFF.toByte()          // Write access: denied
        )

        val ndefSize = ndefFile.size
        return byteArrayOf(
            0x00, 0x0F,             // CC length = 15
            0x20,                   // Mapping version 2.0
            0x00, 0x3B,            // Max R-APDU = 59
            0x00, 0x34,            // Max C-APDU = 52
            0x04, 0x06,            // NDEF TLV: type=4, length=6
            0xE1.toByte(), 0x04,   // NDEF file ID
            (ndefSize shr 8).toByte(), (ndefSize and 0xFF).toByte(),
            0x00,                   // Read access: open
            0xFF.toByte()          // Write access: denied
        )
    }

    /** Build the NDEF file: 2-byte length prefix + NDEF message bytes. */
    private fun buildNdefFile(): ByteArray? {
        val ndef = ndefMessageBytes ?: return null
        val len = ndef.size
        return byteArrayOf((len shr 8).toByte(), (len and 0xFF).toByte()) + ndef
    }

    override fun onDeactivated(reason: Int) {
        // No-op
    }
}
