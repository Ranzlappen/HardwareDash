package dev.ranzlappen.gadget.feature.radios.ir

import androidx.compose.runtime.Immutable

@Immutable
data class IrState(
    val hasEmitter: Boolean = false,
    val supportedFrequencies: List<IntRange> = emptyList(),
    val pendingProtocol: IrProtocol = IrProtocol.NEC,
    val pendingPayload: String = "",
    val pendingCarrierHz: Int = 38_000,
    val pendingRepeats: Int = 1,
    val isTransmitting: Boolean = false,
    val lastTransmitError: String? = null,
    val lastTransmitOk: Boolean = false,
)
