package com.gadget.permissions

import android.Manifest
import android.os.Build

/**
 * Single source of truth for the runtime + special permissions the
 * app may need. Consumed by [PermissionsOnboardingCoordinator] and
 * the existing permission-status surface in `BugReportScreen`.
 */
object RequiredPermissions {

    /**
     * Runtime permissions that should be requested via
     * `ActivityResultContracts.RequestMultiplePermissions()`. Filtered
     * by SDK level inside [runtimePermissionsForSdk].
     */
    fun runtimePermissionsForSdk(sdkInt: Int): List<String> {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACTIVITY_RECOGNITION,
        )
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        if (sdkInt >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_CONNECT
            list += Manifest.permission.BLUETOOTH_SCAN
        }
        return list
    }

    /**
     * Ordered list of "special permission" steps the onboarding flow
     * walks through after the runtime batch. Each entry's [needsRequest]
     * predicate decides whether to launch that step's intent.
     */
    val specialPermissionSteps: List<SpecialPermissionStep> = SpecialPermissionStep.allInDefaultOrder
}
