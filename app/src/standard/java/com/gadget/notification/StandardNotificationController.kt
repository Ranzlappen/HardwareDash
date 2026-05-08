package com.gadget.notification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Notification controller. Every privileged method
 * returns [NotificationControllerResult.Unsupported].
 */
@Singleton
class StandardNotificationController @Inject constructor() : NotificationController {

    override suspend fun overrideStickyChannel(
        config: StickyOverrideConfig,
    ): NotificationControllerResult = NotificationControllerResult.Unsupported

    override suspend fun grantListenerAccess(): NotificationControllerResult =
        NotificationControllerResult.Unsupported

    override suspend fun showLockScreenOverlay(
        config: LockScreenOverlayConfig,
    ): NotificationControllerResult = NotificationControllerResult.Unsupported

    override suspend fun resetAllNotificationMutations(): NotificationControllerResult =
        NotificationControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertOnScreenExit(): NotificationControllerResult =
        NotificationControllerResult.ResetCompleted(restored = 0, failed = 0)
}
