package dev.ranzlappen.gadget.feature.motion

data class MotionState(
    val hasGyroscope: Boolean = false,
    val hasStepCounter: Boolean = false,
    val hasMotionDetect: Boolean = false,
    val rotationRate: Float = 0f,
    val stepCount: Float = 0f,
    val motionDetected: Boolean = false,
    val activityPermissionGranted: Boolean = false,
    val isRootedFlavor: Boolean = false,
)
