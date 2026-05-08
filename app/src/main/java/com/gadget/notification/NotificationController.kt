package com.gadget.notification

/**
 * Rooted-only Notification capability surface. Standard flavor returns
 * [NotificationControllerResult.Unsupported] for every method.
 *
 * Baseline `NotificationManager` / `KeyguardManager` calls in
 * `LockScreenScreen` continue to flow through unchanged.
 */
interface NotificationController {

    /**
     * Raise the importance of [config.channelId] from `LOW` to `HIGH`
     * via `cmd notification`. Snapshot+restore via the shared mutation
     * log so screen exit auto-reverts.
     */
    suspend fun overrideStickyChannel(config: StickyOverrideConfig): NotificationControllerResult

    /**
     * Grant the in-app `RootedNotificationListenerService` access via
     * `cmd notification allow_listener`. Auto-revert on screen dispose.
     * The granted component is hard-coded to the in-app service —
     * callers cannot pass an arbitrary component name.
     */
    suspend fun grantListenerAccess(): NotificationControllerResult

    /**
     * Show a `TYPE_SYSTEM_ALERT` overlay above the keyguard for a
     * bounded duration. Hard 60 s ceiling. Auto-revert
     * (`WindowManager.removeView`) on screen dispose or duration
     * expiry, whichever fires first.
     */
    suspend fun showLockScreenOverlay(config: LockScreenOverlayConfig): NotificationControllerResult

    /** Reverts every Notification-surface mutation. */
    suspend fun resetAllNotificationMutations(): NotificationControllerResult

    /**
     * Auto-revert path called on `LockScreenScreen` dispose. Filters
     * by `cmd-notification://listener/` + `wm-overlay://` prefixes.
     */
    suspend fun revertOnScreenExit(): NotificationControllerResult
}
