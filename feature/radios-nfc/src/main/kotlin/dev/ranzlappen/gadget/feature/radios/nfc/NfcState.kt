package dev.ranzlappen.gadget.feature.radios.nfc

import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagInfo
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteMode
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteOutcome
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate

data class NfcState(
    val adapterPresent: Boolean = false,
    val adapterEnabled: Boolean = false,
    val isRootedFlavor: Boolean = false,
    /** Last tag read (v3 B11), null until one is presented. */
    val tag: NfcTagInfo? = null,
    val readingTag: Boolean = false,
    val writeMode: NfcWriteMode = NfcWriteMode.Text,
    val writeText: String = "",
    /** The next presented tag gets [writeText] written instead of just read. */
    val writeArmed: Boolean = false,
    val writeOutcome: NfcWriteOutcome? = null,
    val hcePayload: String = "",
    val hceMode: NfcHceMode = NfcHceMode.NONE,
    val templates: List<NfcTemplate> = emptyList(),
    val selectedTemplate: NfcTemplate? = null,
    val templateValues: Map<String, String> = emptyMap(),
    val showTemplatePicker: Boolean = false,
) {
    val canWrite: Boolean get() = writeText.isNotBlank()
}

enum class NfcHceMode { NONE, TEXT, URL }
