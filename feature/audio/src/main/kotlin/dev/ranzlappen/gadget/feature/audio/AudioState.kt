package dev.ranzlappen.gadget.feature.audio

import android.net.Uri

data class AudioState(
    val permissionGranted: Boolean = false,
    val isRecording: Boolean = false,
    val recordings: List<Recording> = emptyList(),
    val playingUri: Uri? = null,
    val error: String? = null,
    val isRootedFlavor: Boolean = false,
)
