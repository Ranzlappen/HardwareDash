package dev.ranzlappen.gadget.feature.lock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a [DeviceLockController.lockNow] call. */
sealed class DeviceLockResult {
    /** `DevicePolicyManager.lockNow()` succeeded — the screen is locked. */
    data object Ok : DeviceLockResult()

    /** The device admin isn't active yet; the user must activate it first. */
    data object NotAdmin : DeviceLockResult()

    /** The policy service was unavailable or refused the call. */
    data class Error(val message: String) : DeviceLockResult()
}

/**
 * Standard-tier "lock the device now" via [DevicePolicyManager] and
 * [GadgetDeviceAdminReceiver]. **No root required** — once the user activates
 * the admin (holding only the force-lock policy), `lockNow()` locks the screen
 * on any device.
 *
 * The screen drives activation through [adminActivationIntent] (the system
 * "activate device admin" prompt) and re-reads [isAdminActive] on resume; the
 * ViewModel calls [lockNow] behind a confirmation.
 */
@Singleton
class DeviceLockController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val policyManager: DevicePolicyManager?
        get() = ContextCompat.getSystemService(context, DevicePolicyManager::class.java)

    /** The admin component this app registers (the force-lock receiver). */
    val adminComponent: ComponentName
        get() = ComponentName(context, GadgetDeviceAdminReceiver::class.java)

    /** True once the user has activated the device admin. */
    fun isAdminActive(): Boolean = policyManager?.isAdminActive(adminComponent) == true

    /**
     * The intent that opens the system "activate device admin" screen, with a
     * human [explanation] of why the app wants it (lock-on-demand only).
     */
    fun adminActivationIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)

    /** Lock the screen now — [DeviceLockResult.NotAdmin] if not yet activated. */
    fun lockNow(): DeviceLockResult {
        val manager = policyManager ?: return DeviceLockResult.Error("Device policy service unavailable")
        if (!manager.isAdminActive(adminComponent)) return DeviceLockResult.NotAdmin
        return try {
            manager.lockNow()
            DeviceLockResult.Ok
        } catch (e: SecurityException) {
            DeviceLockResult.Error(e.message ?: "Lock refused")
        }
    }
}
