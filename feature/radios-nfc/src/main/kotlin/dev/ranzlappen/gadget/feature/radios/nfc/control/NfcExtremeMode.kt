package dev.ranzlappen.gadget.feature.radios.nfc.control

/**
 * Send an arbitrary NCI command to the NFC controller via vendor sysfs
 * (e.g. `/sys/class/nfc/nfc0/cmd` or `/sys/class/nfc/nfc0/nci_cmd`).
 * The impl enforces a hard 256-byte payload ceiling and a 5-second
 * read-timeout regardless of what the caller passes.
 */
data class RawNciCommandConfig(
    val payloadHex: String,
)
