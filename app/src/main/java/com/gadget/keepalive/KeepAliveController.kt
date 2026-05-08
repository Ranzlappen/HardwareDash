package com.gadget.keepalive

/**
 * Persistent keep-alive capability surface. Both flavors implement
 * this — the standard flavor surfaces the system battery-opt
 * exemption intent; the rooted flavor automates Doze whitelisting +
 * `pm grant` of a hard-listed set of normal permissions.
 *
 * Either way, [enable] starts the shared [com.gadget.services.PersistentKeepAliveService].
 */
interface KeepAliveController {

    /**
     * Starts the keep-alive foreground service. On rooted, also
     * issues `cmd deviceidle whitelist +<own-pkg>` and `pm grant` of
     * the allow-listed normal permissions. On standard, returns
     * [KeepAliveControllerResult.UserBatteryOptExemptionRequested]
     * so the caller knows to fire the system intent.
     */
    suspend fun enable(): KeepAliveControllerResult

    /**
     * Stops the foreground service and reverts every keep-alive
     * mutation registered with the shared mutation log. Standard
     * impl just stops the service.
     */
    suspend fun disable(): KeepAliveControllerResult

    /**
     * Canonical hook called by the future Batch-11 "Emergency Reset
     * All Root Mutations" button. Same as [disable] today, but kept
     * as a separate method so the future global reset code path is
     * stable.
     */
    suspend fun disableAndStopService(): KeepAliveControllerResult

    /**
     * Standard-flavor users may want to flip the "Keep Alive" toggle
     * back on after granting battery-opt exemption. This re-emits the
     * intent on demand. Rooted impl is a no-op (returns
     * [KeepAliveControllerResult.Ok]).
     */
    suspend fun requestUserBatteryOptExemption(): KeepAliveControllerResult

    /** Reverts every keep-alive mutation. Same as [disable] semantics. */
    suspend fun resetAllKeepAliveMutations(): KeepAliveControllerResult
}
