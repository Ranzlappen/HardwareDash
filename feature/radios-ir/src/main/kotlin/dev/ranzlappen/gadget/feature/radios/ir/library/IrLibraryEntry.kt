package dev.ranzlappen.gadget.feature.radios.ir.library

import kotlinx.serialization.Serializable

@Serializable
data class IrLibraryBrand(
    val brand: String,
    val category: String,
    val signals: List<IrLibrarySignal>,
)

@Serializable
data class IrLibrarySignal(
    val name: String,
    val protocol: String,
    val payload: String,
    val carrierHz: Int = 38_000,
    val repeats: Int = 1,
)
