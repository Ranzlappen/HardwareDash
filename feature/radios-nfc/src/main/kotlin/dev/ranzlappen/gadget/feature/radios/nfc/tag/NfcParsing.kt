package dev.ranzlappen.gadget.feature.radios.nfc.tag

/**
 * Pure NFC decoding (v3 B11): NDEF records, NTAG chip identification from
 * GET_VERSION, and write-protection interpretation of raw NTAG pages. No
 * Android types, so `NfcParsingTest` covers it on the JVM; [NfcTagIo] does
 * the radio I/O and hands the bytes here.
 */
object NfcParsing {

    // NdefRecord TNF values (NFC Forum NDEF spec, mirrored by android.nfc.NdefRecord).
    const val TNF_EMPTY: Short = 0
    const val TNF_WELL_KNOWN: Short = 1
    const val TNF_MIME_MEDIA: Short = 2
    const val TNF_ABSOLUTE_URI: Short = 3
    const val TNF_EXTERNAL_TYPE: Short = 4

    private const val MAX_HEX_BYTES = 64

    fun hex(bytes: ByteArray, separator: String = ""): String =
        bytes.joinToString(separator) { "%02X".format(it.toInt() and 0xFF) }

    fun parseRecord(tnf: Short, type: ByteArray, payload: ByteArray): NdefRecordView = when (tnf) {
        TNF_EMPTY -> NdefRecordView(NdefRecordKind.Empty, "")
        TNF_WELL_KNOWN -> when (String(type, Charsets.US_ASCII)) {
            "T" -> parseText(payload)
            "U" -> NdefRecordView(NdefRecordKind.Uri, parseUri(payload))
            "Sp" -> NdefRecordView(NdefRecordKind.SmartPoster, shortHex(payload))
            else -> NdefRecordView(NdefRecordKind.Other, shortHex(payload), String(type, Charsets.US_ASCII))
        }
        TNF_MIME_MEDIA -> NdefRecordView(
            NdefRecordKind.Mime,
            printableOrHex(payload),
            String(type, Charsets.US_ASCII),
        )
        TNF_ABSOLUTE_URI -> NdefRecordView(NdefRecordKind.Uri, String(type, Charsets.UTF_8))
        TNF_EXTERNAL_TYPE -> NdefRecordView(
            NdefRecordKind.External,
            printableOrHex(payload),
            String(type, Charsets.US_ASCII),
        )
        else -> NdefRecordView(NdefRecordKind.Other, shortHex(payload))
    }

    /** RTD Text: status byte (bit 7 = UTF-16, bits 0–5 = language length), language, text. */
    private fun parseText(payload: ByteArray): NdefRecordView {
        if (payload.isEmpty()) return NdefRecordView(NdefRecordKind.Text, "")
        val status = payload[0].toInt() and 0xFF
        val langLength = (status and 0x3F).coerceAtMost(payload.size - 1)
        val charset = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
        val language = String(payload, 1, langLength, Charsets.US_ASCII)
        val text = String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
        return NdefRecordView(NdefRecordKind.Text, text, language.ifEmpty { null })
    }

    /** RTD URI: identifier-code byte (prefix abbreviation) + UTF-8 rest. */
    fun parseUri(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val code = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIXES.getOrElse(code) { "" }
        return prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
    }

    /** NFC Forum tag type from android.nfc.tech.Ndef#getType, e.g. "Type 2". */
    fun ndefTypeLabel(type: String?): String? = when (type) {
        null -> null
        "org.nfcforum.ndef.type1" -> "Type 1"
        "org.nfcforum.ndef.type2" -> "Type 2"
        "org.nfcforum.ndef.type3" -> "Type 3"
        "org.nfcforum.ndef.type4" -> "Type 4"
        "com.nxp.ndef.mifareclassic" -> "MIFARE Classic"
        else -> type.substringAfterLast('.')
    }

    /**
     * NXP GET_VERSION (0x60) response → chip. Byte 1 = vendor (0x04 NXP),
     * byte 2 = product type (0x04 NTAG, 0x03 Ultralight), byte 6 = storage size.
     */
    fun identifyNtag(version: ByteArray): NtagChip? {
        if (version.size < 8 || version[1].toInt() and 0xFF != 0x04) return null
        val product = version[2].toInt() and 0xFF
        val storage = version[6].toInt() and 0xFF
        return when (product to storage) {
            0x04 to 0x0B -> NtagChip("NTAG210", totalPages = 20, userBytes = 48, dynamicLockPage = -1, configPage = 0x10)
            0x04 to 0x0E -> NtagChip("NTAG212", totalPages = 41, userBytes = 128, dynamicLockPage = 0x24, configPage = 0x25)
            0x04 to 0x0F -> NtagChip("NTAG213", totalPages = 45, userBytes = 144, dynamicLockPage = 0x28, configPage = 0x29)
            0x04 to 0x11 -> NtagChip("NTAG215", totalPages = 135, userBytes = 504, dynamicLockPage = 0x82, configPage = 0x83)
            0x04 to 0x13 -> NtagChip("NTAG216", totalPages = 231, userBytes = 888, dynamicLockPage = 0xE2, configPage = 0xE3)
            0x03 to 0x0B -> NtagChip("MIFARE Ultralight EV1 (MF0UL11)", totalPages = 20, userBytes = 48, dynamicLockPage = -1, configPage = 0x10)
            0x03 to 0x0E -> NtagChip("MIFARE Ultralight EV1 (MF0UL21)", totalPages = 41, userBytes = 128, dynamicLockPage = 0x24, configPage = 0x25)
            else -> null
        }
    }

    /**
     * Interprets NTAG / Ultralight registers.
     *
     * @param pages2to5 16 bytes from READ(page 2): page 2 bytes 2–3 are the
     *   static lock bytes, page 3 is the capability container (CC byte 3 =
     *   write access: 0x00 open, 0x0F read-only).
     * @param dynamicLock READ(dynamic lock page), first 3 bytes used; null if
     *   the chip has none or it could not be read.
     * @param config READ(CFG0 page): byte 3 = AUTH0, byte 4 = ACCESS; null if
     *   unknown chip or unreadable.
     */
    fun interpretProtection(
        pages2to5: ByteArray?,
        chip: NtagChip?,
        dynamicLock: ByteArray?,
        config: ByteArray?,
    ): TagProtection {
        val details = mutableListOf<String>()
        var lockBits: Boolean? = null
        var readOnly: Boolean? = null
        if (pages2to5 != null && pages2to5.size >= 8) {
            val staticLock = ((pages2to5[2].toInt() and 0xFF) shl 8) or (pages2to5[3].toInt() and 0xFF)
            val ccAccess = pages2to5[7].toInt() and 0xFF
            lockBits = staticLock != 0
            readOnly = ccAccess and 0x0F != 0
            details += "Static lock 0x%04X".format(staticLock)
            details += "CC write access 0x%02X".format(ccAccess)
        }
        if (dynamicLock != null && dynamicLock.size >= 3) {
            val dyn = ((dynamicLock[0].toInt() and 0xFF) shl 16) or
                ((dynamicLock[1].toInt() and 0xFF) shl 8) or
                (dynamicLock[2].toInt() and 0xFF)
            if (dyn != 0) lockBits = true
            details += "Dynamic lock 0x%06X".format(dyn)
        }
        var password: Boolean? = null
        if (chip != null && config != null && config.size >= 5) {
            val auth0 = config[3].toInt() and 0xFF
            val access = config[4].toInt() and 0xFF
            // AUTH0 beyond the last page disables protection (factory 0xFF).
            password = auth0 < chip.totalPages
            val prot = if (access and 0x80 != 0) "read+write" else "write"
            details += "AUTH0 0x%02X".format(auth0) +
                if (password) " (protects $prot from page $auth0)" else " (off)"
        }
        return TagProtection(lockBits, readOnly, password, details)
    }

    /** Adds `https://` when a URL has no scheme, so "example.com" opens in a browser. */
    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return trimmed
        return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
    }

    private fun printableOrHex(bytes: ByteArray): String {
        val text = String(bytes, Charsets.UTF_8)
        val printable = text.isNotEmpty() && text.all { !it.isISOControl() || it == '\n' || it == '\r' || it == '\t' } &&
            '�' !in text
        return if (printable) text else shortHex(bytes)
    }

    private fun shortHex(bytes: ByteArray): String =
        if (bytes.size <= MAX_HEX_BYTES) hex(bytes, " ") else hex(bytes.copyOf(MAX_HEX_BYTES), " ") + " …"

    /** NFC Forum URI RTD identifier codes 0x00–0x23. */
    private val URI_PREFIXES = listOf(
        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
        "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:",
        "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://",
        "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
    )
}
