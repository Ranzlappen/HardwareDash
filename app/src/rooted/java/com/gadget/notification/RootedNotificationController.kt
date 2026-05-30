package com.gadget.notification

import android.content.Context
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val NOTIFICATION_RESET_PREFIXES = listOf(
    "cmd-notification://",
    "wm-overlay://",
)
private val NOTIFICATION_SCREEN_EXIT_PREFIXES = listOf(
    "cmd-notification://listener/",
    "wm-overlay://",
)

@Singleton
class RootedNotificationController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val stickyHelper: StickyOverrideHelper,
    private val listenerHelper: ListenerAccessHelper,
    private val overlayHelper: LockScreenOverlayHelper,
    private val mutationLog: SysfsMutationLog,
) : NotificationController {

    override suspend fun overrideStickyChannel(
        config: StickyOverrideConfig,
    ): NotificationControllerResult =
        runGated(RootFeatureKey.NotificationStickyOverride) {
            stickyHelper.raiseImportance(context.packageName, config.channelId)
        }

    override suspend fun grantListenerAccess(): NotificationControllerResult =
        runGated(RootFeatureKey.NotificationListenerAccess) { listenerHelper.allow() }

    override suspend fun showLockScreenOverlay(
        config: LockScreenOverlayConfig,
    ): NotificationControllerResult =
        runGated(RootFeatureKey.NotificationLockScreenOverlay) { overlayHelper.show(config) }

    override suspend fun resetAllNotificationMutations(): NotificationControllerResult {
        val outcome = mutationLog.revertAll(NOTIFICATION_RESET_PREFIXES)
        return NotificationControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): NotificationControllerResult {
        val outcome = mutationLog.revertAll(NOTIFICATION_SCREEN_EXIT_PREFIXES)
        return NotificationControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> NotificationControllerResult,
    ): NotificationControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is NotificationControllerResult.Ok ||
                it is NotificationControllerResult.ChannelImportanceSnapshot
            ) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> NotificationControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            NotificationControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> NotificationControllerResult.Unsupported
    }
}
