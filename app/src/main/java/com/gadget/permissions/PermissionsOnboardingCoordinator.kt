package com.gadget.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Stateless helpers used by `BugReportScreen` to drive the
 * "Request all missing permissions" onboarding flow. The Composable
 * itself owns the resume-state via `rememberSaveable`; this class
 * exists so the deterministic logic (which permissions are missing,
 * which step is next) can be unit-tested without a Compose runtime.
 */
object PermissionsOnboardingCoordinator {

    /**
     * Returns the runtime permissions that are NOT yet granted, in
     * the same order as [RequiredPermissions.runtimePermissionsForSdk].
     */
    fun missingRuntimePermissions(context: Context): List<String> =
        RequiredPermissions
            .runtimePermissionsForSdk(Build.VERSION.SDK_INT)
            .filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

    /**
     * Returns the special-permission steps that still require a
     * Settings-intent visit, in the same order as
     * [RequiredPermissions.specialPermissionSteps].
     */
    fun pendingSpecialSteps(context: Context): List<SpecialPermissionStep> =
        RequiredPermissions.specialPermissionSteps.filter { step ->
            step.needsRequest(context) && step.buildIntent(context) != null
        }
}
