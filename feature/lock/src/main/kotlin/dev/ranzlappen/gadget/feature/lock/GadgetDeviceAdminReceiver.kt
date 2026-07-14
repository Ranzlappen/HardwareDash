package dev.ranzlappen.gadget.feature.lock

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin receiver backing the standard-tier "lock now" action. Android's
 * [android.app.admin.DevicePolicyManager.lockNow] requires an **active** device
 * admin that holds the force-lock policy (declared in
 * `res/xml/lock_device_admin.xml`); this receiver is that admin.
 *
 * Deliberately behaviourless — the app requests only `force-lock` and never
 * wipes, enforces password rules, or does anything else a device admin *could*.
 * lockNow is device-admin, **not** root: it works in the standard flavor once
 * the user activates the admin from the system prompt.
 */
class GadgetDeviceAdminReceiver : DeviceAdminReceiver()
