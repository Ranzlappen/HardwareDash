package com.hardwaredash.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Administration receiver.
 *
 * Declared in AndroidManifest.xml with the BIND_DEVICE_ADMIN permission and
 * references res/xml/device_admin.xml which declares the "force-lock" policy.
 *
 * Once the user activates Device Admin for this app (shown via
 * ACTION_ADD_DEVICE_ADMIN intent), the app can call:
 *   DevicePolicyManager.lockNow()   → instantly locks the screen
 *
 * The user can always deactivate admin in:
 *   Settings → Security → Device Admin apps
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "HardwareDash Device Admin: enabled ✓",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "HardwareDash Device Admin: disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Called before the admin is disabled — return a warning message that
     * Android will show in its confirmation dialog.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Disabling Device Admin will remove the ability to lock the screen from within HardwareDash."
}
