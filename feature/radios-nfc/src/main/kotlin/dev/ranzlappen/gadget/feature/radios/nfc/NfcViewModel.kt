package dev.ranzlappen.gadget.feature.radios.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcControllerResult
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplateRepository
import java.nio.charset.Charset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the NFC screen (W6 in-screen write-tier surface). */
data class NfcRootToolsState(
    val reset: RootActionState = RootActionState(),
)

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val adapter: NfcAdapterWrapper,
    private val hceState: NfcHceState,
    private val templateRepository: NfcTemplateRepository,
    private val nfcController: NfcController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(NfcState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor))
    val state: StateFlow<NfcState> = _state

    private val _rootTools = MutableStateFlow(NfcRootToolsState())

    /** Live status of the confirm-gated rooted NFC-mutation reset. */
    val rootTools: StateFlow<NfcRootToolsState> = _rootTools.asStateFlow()

    fun onResetMutations() {
        viewModelScope.launch {
            _rootTools.update { it.copy(reset = it.reset.copy(running = true)) }
            val result = nfcController.resetAllNfcMutations()
            _rootTools.update { it.copy(reset = result.toActionState()) }
        }
    }

    private fun NfcControllerResult.toActionState(): RootActionState = when (this) {
        is NfcControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        NfcControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is NfcControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        NfcControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is NfcControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is NfcControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is NfcControllerResult.NciResponse ->
            RootActionState(message = "Response: ${responseHex.take(24)}")
    }

    init {
        _state.update {
            it.copy(
                adapterPresent = adapter.isAvailable(),
                adapterEnabled = adapter.isEnabled(),
                templates = templateRepository.templates,
            )
        }
    }

    fun refresh() {
        _state.update { it.copy(adapterPresent = adapter.isAvailable(), adapterEnabled = adapter.isEnabled()) }
    }

    fun onNewIntent(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val tagId = tag.id.joinToString("") { "%02X".format(it) }
        val techList = tag.techList.joinToString(", ") { it.substringAfterLast('.') }

        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val payload = if (rawMsgs != null && rawMsgs.isNotEmpty()) {
            val msg = rawMsgs[0] as? NdefMessage
            msg?.records?.firstOrNull()?.let { parseNdefRecord(it) }
        } else null

        _state.update {
            it.copy(
                lastTagPayload = payload,
                lastTagFormat = techList,
                lastTagId = tagId,
            )
        }
    }

    fun setHcePayload(text: String) {
        _state.update { it.copy(hcePayload = text) }
    }

    fun activateHce(mode: NfcHceMode) {
        viewModelScope.launch {
            val payload = _state.value.hcePayload
            val ndefBytes = when (mode) {
                NfcHceMode.TEXT -> buildTextNdef(payload)
                NfcHceMode.URL -> buildUrlNdef(payload)
                NfcHceMode.NONE -> null
            }
            hceState.setPayload(ndefBytes)
            _state.update { it.copy(hceMode = mode) }
        }
    }

    fun clearHce() {
        hceState.setPayload(null)
        _state.update { it.copy(hceMode = NfcHceMode.NONE, hcePayload = "") }
    }

    fun openTemplatePicker() = _state.update { it.copy(showTemplatePicker = true) }

    fun closeTemplatePicker() = _state.update {
        it.copy(showTemplatePicker = false, selectedTemplate = null, templateValues = emptyMap())
    }

    fun selectTemplate(template: NfcTemplate) = _state.update {
        it.copy(selectedTemplate = template, templateValues = emptyMap())
    }

    fun setTemplateValue(key: String, value: String) = _state.update {
        it.copy(templateValues = it.templateValues + (key to value))
    }

    fun applyTemplate() {
        val s = _state.value
        val template = s.selectedTemplate ?: return
        val resolved = template.resolve(s.templateValues)
        val mode = when (template.mode.uppercase()) {
            "URL" -> NfcHceMode.URL
            else -> NfcHceMode.TEXT
        }
        _state.update { it.copy(hcePayload = resolved, showTemplatePicker = false, selectedTemplate = null) }
        viewModelScope.launch {
            val ndefBytes = when (mode) {
                NfcHceMode.TEXT -> buildTextNdef(resolved)
                NfcHceMode.URL -> buildUrlNdef(resolved)
                NfcHceMode.NONE -> null
            }
            hceState.setPayload(ndefBytes)
            _state.update { it.copy(hceMode = mode) }
        }
    }

    private fun parseNdefRecord(record: NdefRecord): String? {
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val payload = record.payload
                    val languageCodeLength = payload[0].toInt() and 0x3F
                    String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, Charsets.UTF_8)
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    record.toUri()?.toString()
                } else null
            }
            NdefRecord.TNF_MIME_MEDIA -> String(record.payload, Charset.forName("UTF-8"))
            else -> record.payload.joinToString("") { "%02X".format(it) }
        }
    }

    private fun buildTextNdef(text: String): ByteArray {
        val record = NdefRecord.createTextRecord("en", text)
        val msg = NdefMessage(record)
        return msg.toByteArray()
    }

    private fun buildUrlNdef(url: String): ByteArray {
        val record = NdefRecord.createUri(android.net.Uri.parse(url))
        val msg = NdefMessage(record)
        return msg.toByteArray()
    }
}
