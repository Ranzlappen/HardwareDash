package dev.ranzlappen.gadget.feature.radios.nfc

data class NfcState(
    val adapterPresent: Boolean = false,
    val adapterEnabled: Boolean = false,
    val lastTagPayload: String? = null,
    val lastTagFormat: String? = null,
    val lastTagId: String? = null,
    val hcePayload: String = "",
    val hceMode: NfcHceMode = NfcHceMode.NONE,
)

enum class NfcHceMode { NONE, TEXT, URL }
