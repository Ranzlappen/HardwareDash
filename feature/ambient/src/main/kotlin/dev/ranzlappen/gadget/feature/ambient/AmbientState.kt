package dev.ranzlappen.gadget.feature.ambient

data class AmbientState(
    val luxLevel: Float? = null,
    val sensorAvailable: Boolean = false,
    val isRootedFlavor: Boolean = false,
)
