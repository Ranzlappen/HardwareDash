package dev.ranzlappen.gadget.feature.radios.ir

import kotlinx.serialization.Serializable

/**
 * A named IR signal stored in the user's signal library.
 *
 * [id] is a UUID string assigned at creation time — stable across saves.
 * [payload] interpretation depends on [protocol]:
 *   - NEC: hex string, e.g. `"0x20DF10EF"`
 *   - PRONTO: space-separated hex word pairs, e.g. `"0000 006C 0022 0002 …"`
 *   - RAW: comma-separated microsecond values (mark/space pairs), even count
 * [carrierHz] is ignored for PRONTO (derived from the Pronto frequency code).
 */
@Serializable
data class IrSignal(
    val id: String,
    val name: String,
    val protocol: IrProtocol,
    val payload: String,
    val carrierHz: Int = 38_000,
    val repeats: Int = 1,
)
