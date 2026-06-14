package dev.ranzlappen.gadget.feature.camera

import androidx.compose.runtime.Immutable

@Immutable
data class CameraState(
    val permissionGranted: Boolean = false,
    val latestScan: BarcodeResult? = null,
    val isTorchOn: Boolean = false,
    val error: String? = null,
)
