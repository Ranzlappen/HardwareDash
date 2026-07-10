package dev.ranzlappen.gadget.feature.notification.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.feature.notification.NotificationScreenState
import dev.ranzlappen.gadget.feature.notification.R

/**
 * Notification's [ModuleInfo] — standard capability rows (post/cancel a test
 * notification, the channel inspector, the listener-access opt-in that
 * unlocks `active_notifications`) plus rooted-only rows that read red on the
 * standard flavor with a "requires the rooted app version" message, mirroring
 * `torchModuleInfo` / `vibrationModuleInfo`.
 */
@Composable
internal fun notificationModuleInfo(
    state: NotificationScreenState,
    onOpenListenerSettings: () -> Unit,
): ModuleInfo {
    val needsRootMsg = stringResource(R.string.notification_cap_needs_root)
    return ModuleInfo(
        permissions = buildList {
            // POST_NOTIFICATIONS only exists as a runtime permission on API
            // 33+; below that it's auto-granted, so don't surface it as a
            // perpetually-missing row.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    ModulePermission(
                        permission = android.Manifest.permission.POST_NOTIFICATIONS,
                        label = stringResource(R.string.notification_perm_post_label),
                        rationale = stringResource(R.string.notification_perm_post_rationale),
                        optional = false,
                    ),
                )
            }
        },
        compatibility = OsCompatibility(minSdk = Build.VERSION_CODES.Q),
        firmware = null,
        capabilities = listOf(
            ModuleCapability(
                name = stringResource(R.string.notification_cap_builder_name),
                detail = stringResource(R.string.notification_cap_builder_detail),
                status = {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.notification_cap_ok))
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.notification_cap_channel_inspector_name),
                detail = stringResource(R.string.notification_cap_channel_inspector_detail),
                status = {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.notification_cap_ok))
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.notification_cap_listener_name),
                detail = stringResource(R.string.notification_cap_listener_detail),
                status = {
                    if (state.listenerConnected) {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Success,
                            message = stringResource(R.string.notification_cap_listener_granted),
                        )
                    } else {
                        CapabilityStatus(
                            kind = GadgetStatusKind.Warning,
                            message = stringResource(R.string.notification_cap_listener_not_granted),
                            action = CapabilityAction.Custom(
                                label = stringResource(R.string.notification_cap_listener_open_settings),
                                onClick = onOpenListenerSettings,
                            ),
                        )
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.notification_cap_sticky_override_name),
                detail = stringResource(R.string.notification_cap_sticky_override_detail),
                status = {
                    if (state.isRootedFlavor) {
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.notification_cap_root_ready))
                    } else {
                        CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.notification_cap_listener_grant_name),
                detail = stringResource(R.string.notification_cap_listener_grant_detail),
                status = {
                    if (state.isRootedFlavor) {
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.notification_cap_root_ready))
                    } else {
                        CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                    }
                },
            ),
            ModuleCapability(
                name = stringResource(R.string.notification_cap_overlay_name),
                detail = stringResource(R.string.notification_cap_overlay_detail),
                status = {
                    if (state.isRootedFlavor) {
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.notification_cap_overlay_caveat))
                    } else {
                        CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                    }
                },
            ),
        ),
    )
}
