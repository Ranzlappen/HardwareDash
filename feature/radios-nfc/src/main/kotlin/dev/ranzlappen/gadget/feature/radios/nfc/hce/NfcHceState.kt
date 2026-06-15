package dev.ranzlappen.gadget.feature.radios.nfc.hce

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Singleton shared between NfcViewModel and NfcEmulationService. */
@Singleton
class NfcHceState @Inject constructor() {
    private val _payload = MutableStateFlow<ByteArray?>(null)
    val payload: StateFlow<ByteArray?> = _payload

    fun setPayload(bytes: ByteArray?) { _payload.value = bytes }
}
