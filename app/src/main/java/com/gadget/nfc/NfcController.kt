package com.gadget.nfc

/**
 * Rooted-only NFC capability surface. Standard flavor returns
 * [NfcControllerResult.Unsupported] for every method.
 *
 * The interface deliberately exposes ONLY raw NCI command access — the
 * baseline `NfcAdapter` / `Tag` / `IsoDep` / `NdefMessage` API surface
 * continues to flow through `RadiosScreen` unchanged.
 */
interface NfcController {

    /**
     * Sends a raw NCI command to the NFC controller. Hard 256-byte
     * payload ceiling and 5 s read-timeout enforced inside the helper.
     * `requiresExplicitConfirm = true` on the descriptor.
     */
    suspend fun sendRawNciCommand(config: RawNciCommandConfig): NfcControllerResult

    /** Reverts every NFC-surface mutation. */
    suspend fun resetAllNfcMutations(): NfcControllerResult
}
