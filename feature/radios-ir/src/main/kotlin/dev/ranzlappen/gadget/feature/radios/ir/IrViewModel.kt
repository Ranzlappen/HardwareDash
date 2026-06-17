package dev.ranzlappen.gadget.feature.radios.ir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibraryBrand
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibraryRepository
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibrarySignal
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IrViewModel @Inject constructor(
    private val hardware: IrHardware,
    private val repository: IrSignalRepository,
    private val libraryRepository: IrLibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        IrState(
            hasEmitter = hardware.hasEmitter(),
            supportedFrequencies = hardware.supportedFrequencies(),
        )
    )

    init {
        _state.update { it.copy(libraryBrands = libraryRepository.brands) }
    }
    val state: StateFlow<IrState> = _state.asStateFlow()

    val signals: StateFlow<List<IrSignal>> = repository.signals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setProtocol(protocol: IrProtocol) = _state.update {
        it.copy(pendingProtocol = protocol, lastTransmitError = null, lastTransmitOk = false)
    }

    fun setPayload(payload: String) = _state.update {
        it.copy(pendingPayload = payload, lastTransmitError = null, lastTransmitOk = false)
    }

    fun setCarrierHz(hz: Int) = _state.update { it.copy(pendingCarrierHz = hz) }

    fun setRepeats(repeats: Int) = _state.update { it.copy(pendingRepeats = repeats.coerceIn(1, 10)) }

    fun transmit() {
        val s = _state.value
        if (s.isTransmitting) return
        _state.update { it.copy(isTransmitting = true, lastTransmitError = null, lastTransmitOk = false) }
        viewModelScope.launch {
            val signal = IrSignal(
                id = "pending",
                name = "Pending",
                protocol = s.pendingProtocol,
                payload = s.pendingPayload,
                carrierHz = s.pendingCarrierHz,
                repeats = s.pendingRepeats,
            )
            val error = hardware.transmit(signal)
            _state.update { it.copy(isTransmitting = false, lastTransmitError = error, lastTransmitOk = error == null) }
        }
    }

    fun replay(signal: IrSignal) {
        _state.update {
            it.copy(
                pendingProtocol = signal.protocol,
                pendingPayload = signal.payload,
                pendingCarrierHz = signal.carrierHz,
                pendingRepeats = signal.repeats,
            )
        }
        transmit()
    }

    fun saveSignal(name: String) {
        val s = _state.value
        val signal = IrSignal(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Signal" },
            protocol = s.pendingProtocol,
            payload = s.pendingPayload,
            carrierHz = s.pendingCarrierHz,
            repeats = s.pendingRepeats,
        )
        viewModelScope.launch { repository.save(signal) }
    }

    fun delete(signal: IrSignal) {
        viewModelScope.launch { repository.delete(signal.id) }
    }

    fun pasteProto(text: String) {
        val trimmed = text.trim()
        val protocol = when {
            // Pronto: starts with "0000" hex word
            trimmed.startsWith("0000") -> IrProtocol.PRONTO
            // RAW: contains only digits, commas, spaces
            trimmed.matches(Regex("[0-9,\\s]+")) -> IrProtocol.RAW
            // NEC: hex string (0x prefix or bare hex)
            else -> IrProtocol.NEC
        }
        _state.update { it.copy(pendingPayload = trimmed, pendingProtocol = protocol) }
    }

    fun openLibrary() = _state.update { it.copy(showLibrary = true) }

    fun closeLibrary() = _state.update { it.copy(showLibrary = false, selectedBrand = null) }

    fun selectBrand(brand: IrLibraryBrand) = _state.update { it.copy(selectedBrand = brand) }

    fun clearBrandSelection() = _state.update { it.copy(selectedBrand = null) }

    fun importSignal(signal: IrLibrarySignal) {
        val protocol = runCatching { IrProtocol.valueOf(signal.protocol.uppercase()) }
            .getOrDefault(IrProtocol.NEC)
        val irSignal = IrSignal(
            id = UUID.randomUUID().toString(),
            name = signal.name,
            protocol = protocol,
            payload = signal.payload,
            carrierHz = signal.carrierHz,
            repeats = signal.repeats,
        )
        viewModelScope.launch { repository.save(irSignal) }
    }
}
