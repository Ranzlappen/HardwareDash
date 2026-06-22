package dev.ranzlappen.gadget.feature.audio

data class AudioState(
    val permissionGranted: Boolean = false,
    val isRecording: Boolean = false,
    val lastRecordingUri: String? = null,
    val currentDbLevel: Float = 0f,
    val error: String? = null,
    val isRootedFlavor: Boolean = false,
)
