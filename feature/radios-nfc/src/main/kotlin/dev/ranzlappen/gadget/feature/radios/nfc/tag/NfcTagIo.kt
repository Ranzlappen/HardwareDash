package dev.ranzlappen.gadget.feature.radios.nfc.tag

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tag radio I/O (v3 B11): reads what a tag is and holds, and writes an NDEF
 * message to it. Blocking tech calls run on IO; the bytes are decoded by
 * [NfcParsing]. One tech connection at a time, always closed.
 */
@Singleton
class NfcTagIo @Inject constructor() {

    suspend fun read(tag: Tag): NfcTagInfo = withContext(Dispatchers.IO) {
        val techs = tag.techList.map { it.substringAfterLast('.') }
        var info = NfcTagInfo(uid = NfcParsing.hex(tag.id ?: ByteArray(0), ":"), techs = techs)

        Ndef.get(tag)?.let { ndef ->
            // The message cached at discovery needs no connection.
            val message = ndef.cachedNdefMessage
            info = info.copy(
                ndefType = NfcParsing.ndefTypeLabel(ndef.type),
                ndefMaxBytes = ndef.maxSize,
                ndefUsedBytes = message?.byteArrayLength ?: 0,
                writable = ndef.isWritable,
                records = message?.records.orEmpty().map { r ->
                    NfcParsing.parseRecord(r.tnf, r.type ?: ByteArray(0), r.payload ?: ByteArray(0))
                },
            )
        }

        if ("NfcA" in techs && "MifareClassic" !in techs) {
            info = readNtag(tag, info)
        }
        MifareClassic.get(tag)?.let { mfc ->
            info = info.copy(chip = "MIFARE Classic ${mfc.size / 1024}K")
        }
        if (info.chip == null && "IsoDep" in techs) info = info.copy(chip = "ISO-DEP (ISO 14443-4)")
        info
    }

    private fun readNtag(tag: Tag, base: NfcTagInfo): NfcTagInfo {
        val nfcA = NfcA.get(tag) ?: return base
        return try {
            nfcA.connect()
            nfcA.timeout = TIMEOUT_MS
            val chip = runCatching { NfcParsing.identifyNtag(nfcA.transceive(byteArrayOf(0x60))) }.getOrNull()
            // GET_VERSION failing can leave the tag halted; reconnect before reading.
            if (chip == null) reconnect(nfcA)
            val pages = runCatching { nfcA.transceive(byteArrayOf(0x30, 0x02)) }.getOrNull()
            val dynamic = chip?.takeIf { it.dynamicLockPage >= 0 }
                ?.let { runCatching { nfcA.transceive(byteArrayOf(0x30, it.dynamicLockPage.toByte())) }.getOrNull() }
            val config = chip?.let { runCatching { nfcA.transceive(byteArrayOf(0x30, it.configPage.toByte())) }.getOrNull() }
            base.copy(
                chip = chip?.name ?: base.chip,
                protection = if (pages == null) null else NfcParsing.interpretProtection(pages, chip, dynamic, config),
            )
        } catch (_: IOException) {
            base
        } catch (_: SecurityException) {
            base // tag out of date (removed and re-presented)
        } finally {
            runCatching { nfcA.close() }
        }
    }

    private fun reconnect(nfcA: NfcA) {
        runCatching { nfcA.close() }
        runCatching { nfcA.connect() }
    }

    suspend fun write(tag: Tag, message: NdefMessage): NfcWriteOutcome = withContext(Dispatchers.IO) {
        val size = message.byteArrayLength
        try {
            Ndef.get(tag)?.let { ndef ->
                ndef.connect()
                try {
                    return@withContext when {
                        !ndef.isWritable -> NfcWriteOutcome.ReadOnly
                        size > ndef.maxSize -> NfcWriteOutcome.TooLarge(size, ndef.maxSize)
                        else -> {
                            ndef.writeNdefMessage(message)
                            NfcWriteOutcome.Written(size, formatted = false)
                        }
                    }
                } finally {
                    runCatching { ndef.close() }
                }
            }
            NdefFormatable.get(tag)?.let { formatable ->
                formatable.connect()
                try {
                    formatable.format(message)
                    return@withContext NfcWriteOutcome.Written(size, formatted = true)
                } finally {
                    runCatching { formatable.close() }
                }
            }
            NfcWriteOutcome.Unsupported
        } catch (_: TagLostException) {
            NfcWriteOutcome.TagLost
        } catch (e: IOException) {
            NfcWriteOutcome.Failed(e.message ?: e.javaClass.simpleName)
        } catch (e: FormatException) {
            NfcWriteOutcome.Failed(e.message ?: "Malformed NDEF message")
        } catch (_: SecurityException) {
            NfcWriteOutcome.TagLost
        }
    }

    private companion object {
        const val TIMEOUT_MS = 1_000
    }
}
