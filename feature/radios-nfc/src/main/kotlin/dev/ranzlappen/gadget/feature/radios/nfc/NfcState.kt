package dev.ranzlappen.gadget.feature.radios.nfc

import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate

data class NfcState(
    val adapterPresent: Boolean = false,
    val adapterEnabled: Boolean = false,
    val lastTagPayload: String? = null,
    val lastTagFormat: String? = null,
    val lastTagId: String? = null,
    val hcePayload: String = "",
    val hceMode: NfcHceMode = NfcHceMode.NONE,
    val templates: List<NfcTemplate> = emptyList(),
    val selectedTemplate: NfcTemplate? = null,
    val templateValues: Map<String, String> = emptyMap(),
    val showTemplatePicker: Boolean = false,
)

enum class NfcHceMode { NONE, TEXT, URL }
