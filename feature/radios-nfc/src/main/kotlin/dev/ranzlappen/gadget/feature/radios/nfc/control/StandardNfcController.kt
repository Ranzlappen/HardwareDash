package dev.ranzlappen.gadget.feature.radios.nfc.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor NFC controller. Every method returns
 * [NfcControllerResult.Unsupported].
 */
@Singleton
class StandardNfcController @Inject constructor() : NfcController {

    override suspend fun sendRawNciCommand(config: RawNciCommandConfig): NfcControllerResult =
        NfcControllerResult.Unsupported

    override suspend fun resetAllNfcMutations(): NfcControllerResult =
        NfcControllerResult.ResetCompleted(restored = 0, failed = 0)
}
