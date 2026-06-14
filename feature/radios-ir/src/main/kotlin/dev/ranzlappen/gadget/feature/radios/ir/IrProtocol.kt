package dev.ranzlappen.gadget.feature.radios.ir

import kotlinx.serialization.Serializable

@Serializable
enum class IrProtocol { NEC, PRONTO, RAW }
