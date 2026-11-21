package dev.ranzlappen.gadget.feature.camera

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class CameraState(
    val permissionGranted: Boolean = false,
    /** Photo (default, v3 B9) or barcode scanning. */
    val mode: CameraMode = CameraMode.Photo,
    val latestScan: BarcodeResult? = null,
    /** MediaStore URI of the last capture this session. */
    val lastPhotoUri: Uri? = null,
    /** Message of the last failed capture, cleared by the next success. */
    val photoError: String? = null,
    val error: String? = null,
)
