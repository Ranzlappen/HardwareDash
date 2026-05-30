package com.gadget.nfc

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal const val NFC_PAYLOAD_HARD_BYTE_CEILING = 256
internal const val NFC_NCI_TIMEOUT_MILLIS = 5_000L
private val NCI_NODE_CANDIDATES = listOf(
    "/sys/class/nfc/nfc0/cmd",
    "/sys/class/nfc/nfc0/nci_cmd",
    "/sys/class/nfc/nfc0/cmd_buffer",
)
private val NCI_RESPONSE_NODE_CANDIDATES = listOf(
    "/sys/class/nfc/nfc0/rsp",
    "/sys/class/nfc/nfc0/nci_rsp",
    "/sys/class/nfc/nfc0/last_response",
)

/**
 * Writes a raw NCI command payload to the NFC controller's vendor
 * sysfs node (path varies per chipset — Broadcom, NXP, ST21NFC, etc.).
 * Probes the candidate paths and surfaces `Unsupported` cleanly when
 * none accept the write.
 *
 * Hard 256-byte payload ceiling and 5-second read-timeout enforced
 * inside the helper.
 */
@Singleton
class NciCommandHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun send(payloadHex: String): NfcControllerResult {
        val sanitized = payloadHex.replace("\\s".toRegex(), "")
        if (sanitized.length % 2 != 0) {
            return NfcControllerResult.HardwareError("payload must be hex pairs")
        }
        val byteCount = sanitized.length / 2
        if (byteCount > NFC_PAYLOAD_HARD_BYTE_CEILING) {
            return NfcControllerResult.HardwareError(
                "payload $byteCount bytes exceeds ${NFC_PAYLOAD_HARD_BYTE_CEILING}-byte ceiling",
            )
        }
        val targetCmdNode = NCI_NODE_CANDIDATES.firstOrNull { isWritable(it) }
            ?: return NfcControllerResult.Unsupported

        val write = shell.exec(
            command = "echo -ne \"\\x${sanitized.chunked(2).joinToString("\\x")}\" > \"$targetCmdNode\"",
            timeoutMillis = NFC_NCI_TIMEOUT_MILLIS,
        )
        if (!write.isSuccess) {
            return NfcControllerResult.HardwareError("write to $targetCmdNode rejected by kernel")
        }

        val responseNode = NCI_RESPONSE_NODE_CANDIDATES.firstOrNull { isReadable(it) }
        val responseHex = if (responseNode != null) {
            shell.exec("xxd -p \"$responseNode\" 2>/dev/null").stdout.firstOrNull()?.trim().orEmpty()
        } else {
            ""
        }
        return NfcControllerResult.NciResponse(responseHex = responseHex)
    }

    private suspend fun isWritable(path: String): Boolean {
        val probe = shell.exec("test -w \"$path\" && echo ok")
        return probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok"
    }

    private suspend fun isReadable(path: String): Boolean {
        val probe = shell.exec("test -r \"$path\" && echo ok")
        return probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok"
    }
}
