package dev.ranzlappen.gadget.feature.radios.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcController
import dev.ranzlappen.gadget.feature.radios.nfc.control.NfcControllerResult
import dev.ranzlappen.gadget.feature.radios.nfc.control.RawNciCommandConfig
import dev.ranzlappen.gadget.feature.radios.nfc.hce.NfcHceState
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcParsing
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagInbox
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcTagIo
import dev.ranzlappen.gadget.feature.radios.nfc.tag.NfcWriteMode
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplate
import dev.ranzlappen.gadget.feature.radios.nfc.template.NfcTemplateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the NFC screen (W6 in-screen write-tier surface). */
data class NfcRootToolsState(
    val reset: RootActionState = RootActionState(),
    val sendNci: RootActionState = RootActionState(),
)

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val adapter: NfcAdapterWrapper,
    private val hceState: NfcHceState,
    private val templateRepository: NfcTemplateRepository,
    private val nfcController: NfcController,
    private val tagIo: NfcTagIo,
    private val tagInbox: NfcTagInbox,
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

    /**
     * Config-entry: send a raw NCI command (rooted NFC controller path). The
     * caller confirm-gates this (`requiresExplicitConfirm`); the controller
     * enforces the 256-byte payload ceiling and read timeout.
     */
    fun onSendRawNci(payloadHex: String) {
        viewModelScope.launch {
            _rootTools.update { it.copy(sendNci = it.sendNci.copy(running = true)) }
            val result = nfcController.sendRawNciCommand(RawNciCommandConfig(payloadHex = payloadHex))
            _rootTools.update { it.copy(sendNci = result.toActionState()) }
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
        viewModelScope.launch {
            tagInbox.incoming.collect { tag ->
                tagInbox.consumed()
                onTag(tag)
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(adapterPresent = adapter.isAvailable(), adapterEnabled = adapter.isEnabled()) }
    }

    /**
     * A tag was presented (reader mode on the NFC screen, or the activity's
     * NFC intent via [NfcTagInbox]). If a write is armed it is written first,
     * then the tag is read back so the card shows what is on it now.
     */
    fun onTag(tag: Tag) {
        viewModelScope.launch {
            _state.update { it.copy(readingTag = true) }
            val s = _state.value
            if (s.writeArmed && s.canWrite) {
                val outcome = tagIo.write(tag, buildWriteMessage(s.writeMode, s.writeText))
                _state.update { it.copy(writeArmed = false, writeOutcome = outcome) }
            }
            val info = tagIo.read(tag)
            _state.update { it.copy(tag = info, readingTag = false) }
        }
    }

    fun setWriteMode(mode: NfcWriteMode) = _state.update { it.copy(writeMode = mode, writeOutcome = null) }

    fun setWriteText(text: String) = _state.update { it.copy(writeText = text, writeOutcome = null) }

    fun armWrite() = _state.update { if (it.canWrite) it.copy(writeArmed = true, writeOutcome = null) else it }

    fun cancelWrite() = _state.update { it.copy(writeArmed = false) }

    private fun buildWriteMessage(mode: NfcWriteMode, text: String): NdefMessage = NdefMessage(
        when (mode) {
            NfcWriteMode.Text -> NdefRecord.createTextRecord(null, text)
            NfcWriteMode.Url -> NdefRecord.createUri(NfcParsing.normalizeUrl(text))
        },
    )

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
