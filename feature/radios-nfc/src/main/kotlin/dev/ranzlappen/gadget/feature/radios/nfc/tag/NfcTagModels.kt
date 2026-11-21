package dev.ranzlappen.gadget.feature.radios.nfc.tag

/** Kind of one NDEF record, for the label next to its value. */
enum class NdefRecordKind { Text, Uri, Mime, External, SmartPoster, Empty, Other }

/** One decoded NDEF record. [value] is human-readable text (or hex). */
data class NdefRecordView(
    val kind: NdefRecordKind,
    val value: String,
    /** MIME type / external type / language code, when the record has one. */
    val detail: String? = null,
)

/**
 * An NXP NTAG / MIFARE Ultralight EV1 chip identified from GET_VERSION.
 * Page numbers are the chip's datasheet addresses; `-1` = not present.
 */
data class NtagChip(
    val name: String,
    val totalPages: Int,
    val userBytes: Int,
    val dynamicLockPage: Int,
    val configPage: Int,
)

/**
 * Write protection of an NfcA (NTAG / Ultralight) tag, read from its lock
 * bytes, capability container and (if the chip has one) password config.
 * `null` fields could not be read.
 */
data class TagProtection(
    /** Static or dynamic lock bits set — those pages are permanently read-only. */
    val lockBitsSet: Boolean?,
    /** Capability container marks the NDEF area read-only. */
    val readOnlyFlag: Boolean?,
    /** Password protection (AUTH0) is active. */
    val passwordActive: Boolean?,
    /** Raw register values for the curious, e.g. "Static lock 0x0000". */
    val details: List<String>,
) {
    val anyProtection: Boolean
        get() = lockBitsSet == true || readOnlyFlag == true || passwordActive == true
}

/** Everything the NFC screen shows about the last tag it saw. */
data class NfcTagInfo(
    val uid: String,
    val techs: List<String>,
    /** Chip name (e.g. "NTAG215", "MIFARE Classic 1K") when identifiable. */
    val chip: String? = null,
    /** NFC Forum tag type label (e.g. "Type 2"), when the tag speaks NDEF. */
    val ndefType: String? = null,
    val ndefMaxBytes: Int? = null,
    val ndefUsedBytes: Int? = null,
    val writable: Boolean? = null,
    val records: List<NdefRecordView> = emptyList(),
    val protection: TagProtection? = null,
)

/** What a write attempt did. */
sealed interface NfcWriteOutcome {
    data class Written(val bytes: Int, val formatted: Boolean) : NfcWriteOutcome
    data object ReadOnly : NfcWriteOutcome
    data class TooLarge(val bytes: Int, val maxBytes: Int) : NfcWriteOutcome
    data object Unsupported : NfcWriteOutcome
    data object TagLost : NfcWriteOutcome
    data class Failed(val message: String) : NfcWriteOutcome
}

/** What the "Write to tag" card writes. */
enum class NfcWriteMode { Text, Url }
