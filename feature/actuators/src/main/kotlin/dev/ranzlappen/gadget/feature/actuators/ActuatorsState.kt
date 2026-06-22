package dev.ranzlappen.gadget.feature.actuators

data class ActuatorsState(
    val vibratorAvailable: Boolean = false,
    val hasAmplitudeControl: Boolean = false,
    val isRootedFlavor: Boolean = false,
)
