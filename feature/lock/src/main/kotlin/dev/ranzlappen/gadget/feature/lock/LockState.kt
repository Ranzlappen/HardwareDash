package dev.ranzlappen.gadget.feature.lock

data class LockState(
    val isLocked: Boolean = false,
    val isSecure: Boolean = false,
    val hasBiometric: Boolean = false,
    val isRootedFlavor: Boolean = false,
)
